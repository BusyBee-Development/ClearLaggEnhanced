#!/usr/bin/env node
// Reads docs/**/*.md, builds the payload shape the busybeedev.net
// `POST /api/admin/docs/sync` endpoint expects, and posts it.
//
// Env vars (all required):
//   DOCS_SYNC_URL      e.g. https://busybeedev.net/api/admin/docs/sync
//   DOCS_SYNC_API_KEY  bearer token, must match the site's DOCS_SYNC_KEY_<PROJECT_SLUG>
//                      (slug upper-cased, non-alphanumerics as underscores)
//
// Run with `--dry-run` to print the payload instead of posting it (no env vars needed).
//
// Master copy: 01 - Directives/workflow-templates/sync-docs.mjs. Repo copies differ only in the
// per-repo settings block below; change everything else in the master and re-copy.
//
// Zero npm dependencies on purpose: GitHub Actions runners already ship a recent Node, so this
// never needs an install step. Frontmatter parsing is intentionally minimal: it only handles the
// `---\nkey: value\n...\n---` shape every file in docs/ is written with.

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, basename, dirname, relative, resolve, sep } from 'node:path';

// ---- Per-repo settings (the only part that differs between copies) ----

// The project's slug on busybeedev.net, exactly as stored (the site matches it case-sensitively).
const PROJECT_SLUG = 'clearlaggenhanced';

// Folder name -> { title shown in the sidebar, fixed sort position }.
const SECTION_MAP = {
    'getting-started': { title: 'Getting Started', sortOrder: 1 },
    'core-reference': { title: 'Core Reference', sortOrder: 2 },
    'features-modules': { title: 'Features & Modules', sortOrder: 3 },
    configuration: { title: 'Configuration', sortOrder: 4 },
    integrations: { title: 'Integrations', sortOrder: 5 },
    'advanced-optimization': { title: 'Advanced Optimization', sortOrder: 6 },
    support: { title: 'Support', sortOrder: 7 },
};

// Root-level files that are not doc pages.
const EXCLUDED_ROOT_FILES = new Set(['README.md', 'materials.md']);

// true: pages and sections missing from docs/ are deleted from the site too. Leave false when the
// site holds pages that don't come from docs/ (e.g. ClearLaggEnhanced's materials table).
const PRUNE = false;

// ---- End of per-repo settings ----

const DOCS_ROOT = join(import.meta.dirname, '..', '..', 'docs');
const DRY_RUN = process.argv.includes('--dry-run');

function warn(message) {
    // `::warning::` shows up as an annotation on the GitHub Actions run.
    console.warn(process.env.GITHUB_ACTIONS ? `::warning::${message}` : `WARNING: ${message}`);
}

function parseFrontmatter(raw) {
    const match = raw.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/);
    if (!match) {
        throw new Error('Missing or malformed frontmatter block');
    }
    const [, fmBlock, body] = match;
    const fm = {};
    for (const line of fmBlock.split(/\r?\n/)) {
        const kv = line.match(/^(\w[\w-]*):\s*(.*)$/);
        if (kv) fm[kv[1]] = kv[2].trim().replace(/^(['"])(.*)\1$/, '$2');
    }
    return { frontmatter: fm, content: body.trim() };
}

function toKey(filePath) {
    return relative(DOCS_ROOT, filePath).split(sep).join('/');
}

// The site already shows the frontmatter title as the page heading, so a `# Heading` at the top
// of the body would show the title twice. Drop it.
function stripLeadingH1(content, key) {
    const match = content.match(/^#\s+(.+)\r?\n?/);
    if (!match) return content;
    warn(`${key}: removed leading "# ${match[1].trim()}" (the page title already comes from frontmatter)`);
    return content.slice(match[0].length).trim();
}

function loadDoc(filePath, fallbackSortOrder) {
    const raw = readFileSync(filePath, 'utf8');
    const { frontmatter, content } = parseFrontmatter(raw);
    const slug = frontmatter.id ?? basename(filePath, '.md');
    const title = frontmatter.title ?? slug;
    const sortOrder = frontmatter.sidebar_position
        ? Number.parseInt(frontmatter.sidebar_position, 10)
        : fallbackSortOrder;
    return { slug, title, content: stripLeadingH1(content, toKey(filePath)), sortOrder, filePath };
}

// Pages are written with repo-relative links (`../support/faq.md#anchor`) so they work on GitHub,
// but the site serves every page at /docs/<project>/<id>. Rewrite each relative .md link to the
// target page's URL. Fenced code blocks are left alone.
function rewriteLinks(doc, slugByPath) {
    const parts = doc.content.split(/(^```[\s\S]*?^```)/m);
    return parts
        .map((part, i) => {
            if (i % 2 === 1) return part;
            return part.replace(/\]\(<?([^)\s>]+?\.md)(#[^)\s>]*)?>?\)/g, (match, target, anchor = '') => {
                if (/^[a-z][a-z0-9+.-]*:/i.test(target) || target.startsWith('/')) return match;
                const slug = slugByPath.get(toKey(resolve(dirname(doc.filePath), target)));
                if (!slug) {
                    warn(`${toKey(doc.filePath)}: link to ${target} doesn't point at a synced page`);
                    return match;
                }
                return `](/docs/${PROJECT_SLUG}/${slug}${anchor})`;
            });
        })
        .join('');
}

function buildPayload() {
    const entries = readdirSync(DOCS_ROOT, { withFileTypes: true });

    const unsectioned = [];
    const sectionsByFolder = new Map();

    for (const entry of entries) {
        const fullPath = join(DOCS_ROOT, entry.name);

        if (entry.isFile() && entry.name.endsWith('.md')) {
            if (EXCLUDED_ROOT_FILES.has(entry.name)) continue;
            unsectioned.push(loadDoc(fullPath, unsectioned.length));
            continue;
        }

        if (entry.isDirectory()) {
            const sectionMeta = SECTION_MAP[entry.name];
            if (!sectionMeta) {
                warn(`Skipping docs/${entry.name}/: folder not in SECTION_MAP`);
                continue;
            }

            const files = readdirSync(fullPath)
                .filter((f) => f.endsWith('.md') && statSync(join(fullPath, f)).isFile())
                .sort();

            const docs = files.map((f, i) => loadDoc(join(fullPath, f), i + 1));
            docs.sort((a, b) => a.sortOrder - b.sortOrder);

            sectionsByFolder.set(entry.name, {
                title: sectionMeta.title,
                sortOrder: sectionMeta.sortOrder,
                docs,
            });
        }
    }

    const sections = [...sectionsByFolder.values()].sort((a, b) => a.sortOrder - b.sortOrder);
    const allDocs = [...unsectioned, ...sections.flatMap((section) => section.docs)];

    const seen = new Map();
    for (const doc of allDocs) {
        if (seen.has(doc.slug)) {
            throw new Error(`Duplicate page id "${doc.slug}" in ${toKey(seen.get(doc.slug))} and ${toKey(doc.filePath)}`);
        }
        seen.set(doc.slug, doc.filePath);
    }

    const slugByPath = new Map(allDocs.map((doc) => [toKey(doc.filePath), doc.slug]));
    for (const doc of allDocs) {
        doc.content = rewriteLinks(doc, slugByPath);
        delete doc.filePath;
    }

    return { projectSlug: PROJECT_SLUG, prune: PRUNE, unsectioned, sections };
}

async function main() {
    const payload = buildPayload();
    const docCount = payload.unsectioned.length + payload.sections.reduce((n, s) => n + s.docs.length, 0);

    if (DRY_RUN) {
        console.log(JSON.stringify(payload, null, 2));
        return;
    }

    const url = process.env.DOCS_SYNC_URL;
    const apiKey = process.env.DOCS_SYNC_API_KEY;
    if (!url || !apiKey) {
        console.error('DOCS_SYNC_URL and DOCS_SYNC_API_KEY must both be set.');
        process.exit(1);
    }

    console.log(
        `Syncing ${docCount} doc(s) across ${payload.sections.length} section(s) ` +
            `(+ ${payload.unsectioned.length} unsectioned) for project "${payload.projectSlug}"...`,
    );

    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${apiKey}`,
        },
        body: JSON.stringify(payload),
    });

    const text = await res.text();
    if (!res.ok) {
        if (res.status === 404) {
            console.error(`No project with slug "${PROJECT_SLUG}" on the site. Create it there first.`);
        }
        console.error(`Sync failed: HTTP ${res.status}\n${text}`);
        process.exit(1);
    }

    console.log(`Sync succeeded: ${text}`);
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
