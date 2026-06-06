/**
 * The /customblock (alias /cb) command tree. CommandRegistrar builds the Brigadier
 * nodes and delegates each subcommand to one handler per domain in the handlers
 * sub-package. DidYouMean suggests corrections; HelpRegistry maps every command to
 * a help entry.
 *
 * Design rule: NO command monolith. The old CustomBlockCommand grew to ~6,300 lines
 * and must not be repeated (see §9.3).
 */
package com.customblocks.command;
