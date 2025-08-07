package com.glance.codex.platform.paper.command.core;

import com.glance.codex.platform.paper.command.engine.CommandHandler;
import com.google.auto.service.AutoService;
import com.google.inject.Singleton;

@Singleton
@AutoService(CommandHandler.class)
public class CollectionsCommand implements CommandHandler {
}
