#!/usr/bin/env node
// Reads docs/**/*.md, builds the payload shape the busybeedev.net
// `POST /api/admin/docs/sync` endpoint expects, and posts it.
//
// Env vars (all required):
//   DOCS_SYNC_URL      e.g. https://busybeedev.net/api/admin/docs/sync
//   DOCS_SYNC_API_KEY  bearer token, must match the site's DOCS_SYNC_API_KEY
//
// Zero npm dependencies on purpose — this repo is a Java plugin, not a Node
// project, and GitHub Actions runners already ship a recent Node. Frontmatter
// parsing below is intentionally minimal: it only needs to handle the exact
// `---\nkey: value\n...\n---` shape every file in docs/ was written with.

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, basename } from 'node:path';

const DOCS_ROOT = join(import.meta.dirname, '..', '..', 'docs');
const PROJECT_SLUG = 'clearlaggenhanced';

// Folder name -> { title shown in the sidebar, fixed sort position }.
// Must match docs/_sidebar-reference.md — update both together.
const SECTION_MAP = {
    'getting-started': { title: 'Getting Started', sortOrder: 1 },
    'core-reference': { title: 'Core Reference', sortOrder: 2 },
    'features-modules': { title: 'Features & Modules', sortOrder: 3 },
    configuration: { title: 'Configuration', sortOrder: 4 },
    integrations: { title: 'Integrations', sortOrder: 5 },
    'advanced-optimization': { title: 'Advanced Optimization', sortOrder: 6 },
    support: { title: 'Support', sortOrder: 7 },
};

// Root-level files that are meta/not real doc pages, or (materials.md)
// intentionally excluded because the live site ignores Doc.content for that
// slug and renders a separate MaterialReference table instead — syncing it
// would just write content nothing ever displays.
const EXCLUDED_ROOT_FILES = new Set(['README.md', '_sidebar-reference.md', 'materials.md']);

function parseFrontmatter(raw) {
    const match = raw.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?([\s\S]*)$/);
    if (!match) {
        throw new Error('Missing or malformed frontmatter block');
    }
    const [, fmBlock, body] = match;
    const fm = {};
    for (const line of fmBlock.split(/\r?\n/)) {
        const kv = line.match(/^(\w[\w-]*):\s*(.*)$/);
        if (kv) fm[kv[1]] = kv[2].trim();
    }
    return { frontmatter: fm, content: body.trim() };
}

function loadDoc(filePath, fallbackSortOrder) {
    const raw = readFileSync(filePath, 'utf8');
    const { frontmatter, content } = parseFrontmatter(raw);
    const slug = frontmatter.id ?? basename(filePath, '.md');
    const title = frontmatter.title ?? slug;
    const sortOrder = frontmatter.sidebar_position
        ? Number.parseInt(frontmatter.sidebar_position, 10)
        : fallbackSortOrder;
    return { slug, title, content, sortOrder };
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
                console.warn(`Skipping unknown docs/ subfolder (not in SECTION_MAP): ${entry.name}`);
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

    return { projectSlug: PROJECT_SLUG, unsectioned, sections };
}

async function main() {
    const url = process.env.DOCS_SYNC_URL;
    const apiKey = process.env.DOCS_SYNC_API_KEY;

    if (!url || !apiKey) {
        console.error('DOCS_SYNC_URL and DOCS_SYNC_API_KEY must both be set.');
        process.exit(1);
    }

    const payload = buildPayload();
    const docCount = payload.unsectioned.length + payload.sections.reduce((n, s) => n + s.docs.length, 0);
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
        console.error(`Sync failed: HTTP ${res.status}\n${text}`);
        process.exit(1);
    }

    console.log(`Sync succeeded: ${text}`);
}

main().catch((err) => {
    console.error(err);
    process.exit(1);
});
