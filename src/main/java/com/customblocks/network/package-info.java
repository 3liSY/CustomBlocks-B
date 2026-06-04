/**
 * Networking layer. NetworkManager registers and dispatches all payloads;
 * ServerPackGenerator builds the resource-pack ZIP in memory; ResourcePackServer
 * is an embedded HTTP server that serves exactly one endpoint
 * (/customblocks_pack.zip) with no directory traversal.
 *
 * Payload record types live in the payloads sub-package; debounce/queue helpers
 * live in the sync sub-package.
 */
package com.customblocks.network;
