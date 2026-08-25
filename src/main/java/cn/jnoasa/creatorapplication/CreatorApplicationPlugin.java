package cn.jnoasa.creatorapplication;

import org.springframework.stereotype.Component;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.Scheme;

@Component
public class CreatorApplicationPlugin extends BasePlugin {
    private final SchemeManager schemeManager;

    public CreatorApplicationPlugin(PluginContext pluginContext, SchemeManager schemeManager) {
        super(pluginContext);
        this.schemeManager = schemeManager;
    }

    @Override
    public void start() {
        schemeManager.register(CreatorApplication.class);
    }

    @Override
    public void stop() {
        schemeManager.unregister(Scheme.buildFromType(CreatorApplication.class));
    }
}
