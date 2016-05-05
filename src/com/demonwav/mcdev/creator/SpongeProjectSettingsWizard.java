package com.demonwav.mcdev.creator;

import com.demonwav.mcdev.platform.sponge.SpongeProjectConfiguration;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import org.apache.commons.lang.WordUtils;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SpongeProjectSettingsWizard extends ModuleWizardStep {

    private JTextField pluginNameField;
    private JTextField pluginVersionField;
    private JTextField mainClassField;
    private JPanel panel;
    private JLabel title;

    private final SpongeProjectConfiguration settings = new SpongeProjectConfiguration();
    private final MinecraftProjectCreator creator;

    public SpongeProjectSettingsWizard(MinecraftProjectCreator creator) {
        this.creator = creator;
    }

    @Override
    public JComponent getComponent() {
        pluginNameField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));
        pluginVersionField.setText(creator.getVersion());
        mainClassField.setText(WordUtils.capitalizeFully(creator.getArtifactId()));

        return panel;
    }

    @Override
    public void updateDataModel() {

    }

    @Override
    public void onStepLeaving() {
        super.onStepLeaving();
        settings.pluginName = pluginNameField.getText();
        settings.pluginVersion = pluginVersionField.getText();
        settings.mainClass = mainClassField.getText();
        // TODO: set settings
        creator.setSettings(settings);
    }
}