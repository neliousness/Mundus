/*
 * Copyright (c) 2016. See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mbrlabs.mundus.ui.modules.inspector.terrain;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.layout.GridGroup;
import com.kotcrab.vis.ui.util.dialog.DialogUtils;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.mbrlabs.mundus.core.Inject;
import com.mbrlabs.mundus.core.Mundus;
import com.mbrlabs.mundus.tools.ToolManager;
import com.mbrlabs.mundus.tools.brushes.TerrainBrush;
import com.mbrlabs.mundus.ui.Ui;
import com.mbrlabs.mundus.ui.widgets.FaTextButton;

/**
 * @author Marcus Brummer
 * @version 30-01-2016
 */
public class TerrainBrushTable extends VisTable {

    private TerrainComponentWidget parent;

    private FaTextButton sphereBrushBtn;
    private TerrainBrush.BrushMode brushMode;

    private GridGroup grid;

    @Inject
    private ToolManager toolManager;

    public TerrainBrushTable(TerrainComponentWidget parent) {
        super();
        Mundus.inject(this);
        this.parent = parent;
        align(Align.left);
        add(new VisLabel("Brushes:")).padBottom(10).row();

        grid = new GridGroup(40, 5);
        for(TerrainBrush brush : toolManager.terrainBrushes) {
            grid.addActor(new BrushItem(brush));
        }

        add(grid).expand().fill().row();
    }

    public TerrainBrushTable(TerrainComponentWidget parent, TerrainBrush.BrushMode mode) {
        this(parent);
        this.brushMode = mode;
    }

    public TerrainBrush.BrushMode getBrushMode() {
        return brushMode;
    }

    public void setBrushMode(TerrainBrush.BrushMode brushMode) {
        this.brushMode = brushMode;
    }

    public void activateBrush(TerrainBrush brush) {
        try {
            brush.setMode(brushMode);
            toolManager.activateTool(brush);
            brush.setTerrain(parent.component.getTerrain());
        } catch (TerrainBrush.ModeNotSupportedException e) {
            e.printStackTrace();
            DialogUtils.showErrorDialog(Ui.getInstance(), e.getMessage());
        }

    }

    /**
     *
     */
    private class BrushItem extends VisTable {

        public BrushItem(final TerrainBrush brush) {
            super();
            add(new FaTextButton(brush.getIconFont()));

            addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    activateBrush(brush);
                }
            });

        }
    }

}
