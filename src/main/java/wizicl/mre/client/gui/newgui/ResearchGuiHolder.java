package wizicl.mre.client.gui.newgui;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import net.minecraft.tileentity.TileEntity;


public class ResearchGuiHolder extends TileEntity implements IGuiHolder<PosGuiData> {

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        ModularPanel panel = ModularPanel.defaultPanel("ResearchGui");
        panel.bindPlayerInventory();


        return panel;
    }
}
