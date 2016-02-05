package com.mbrlabs.mundus.tools.brushes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.mbrlabs.mundus.commons.terrain.SplatMap;
import com.mbrlabs.mundus.commons.terrain.SplatTexture;
import com.mbrlabs.mundus.commons.terrain.Terrain;
import com.mbrlabs.mundus.core.project.ProjectContext;
import com.mbrlabs.mundus.tools.Tool;

/**
 * A Terrain Brush can modify the terrain in various ways (BrushMode).
 *
 * This includes the height of every vertex in the terrain grid & according
 * splatmap.
 *
 * @author Marcus Brummer
 * @version 30-01-2016
 */
public abstract class TerrainBrush extends Tool {

    /**
     * Defines the draw mode of a brush.
     */
    public enum BrushMode {
        /** Raises or lowers the terrain height. */
        RAISE_LOWER,
        /** Sets all vertices of the selection to a specified height. */
        FLATTEN,
        /** TBD */
        SMOOTH,
        /** Paints on the splatmap of the terrain. */
        PAINT
    }

    /**
     * Defines two actions (and it's key codes) every brush and every mode can have.
     *
     * For instance the RAISE_LOWER mode has 'raise' has PRIMARY action and 'lower' as secondary.
     * Pressing the keycode of the secondary & the primary key enables the secondary action.
     **/
    public static enum BrushAction {
        PRIMARY(Input.Buttons.LEFT),
        SECONDARY(Input.Keys.SHIFT_LEFT);

        public final int code;

        private BrushAction(int levelCode) {
            this.code = levelCode;
        }

    }

    /**
     * Thrown if a the brush is set to a mode, which it currently does not support.
     */
    public class ModeNotSupportedException extends Exception {
        public ModeNotSupportedException(String message) {
            super(message);
        }
    }

    protected Vector3 brushPos = new Vector3();

    // brush settings
    protected SplatTexture.Channel paintChannel;
    protected BrushMode mode;
    protected Terrain terrain;
    protected float radius;
    protected float strength = 0.5f;
    protected float heightSample = 0f;

    // used for brush visualization
    private Model sphereModel;
    private ModelInstance sphereModelInstance;
    private BoundingBox boundingBox = new BoundingBox();
    private int lastMousePosIndicator = 0;

    // the pixmap brush
    private Pixmap brushPixmap;
    private int pixmapCenter;
    private Color c0 = new Color();

    // used fora calculations
    private Vector2 c = new Vector2();
    private Vector2 p = new Vector2();
    private Vector2 v = new Vector2();
    protected Vector3 tVec0 = new Vector3();

    public TerrainBrush(ProjectContext projectContext, Shader shader, ModelBatch batch, FileHandle pixmapBrush) {
        super(projectContext, shader, batch);

        ModelBuilder modelBuilder = new ModelBuilder();
        sphereModel = modelBuilder.createSphere(1, 1, 1, 30, 30, new Material(), VertexAttributes.Usage.Position);
        sphereModelInstance = new ModelInstance(sphereModel);
        sphereModelInstance.calculateBoundingBox(boundingBox);
        scale(15);

        brushPixmap = new Pixmap(pixmapBrush);
        pixmapCenter = brushPixmap.getWidth() / 2;
    }

    public BrushAction getAction() {
        final boolean primary = Gdx.input.isButtonPressed(BrushAction.PRIMARY.code);
        final boolean secondary = Gdx.input.isKeyPressed(BrushAction.SECONDARY.code);

        if(primary && secondary) {
            return BrushAction.SECONDARY;
        } else if(primary) {
            return BrushAction.PRIMARY;
        }

        return null;
    }

    @Override
    public void act() {
        BrushAction action = getAction();
        if(action == null) return;
        if(terrain == null) return;

        // sample height
        if(action == BrushAction.SECONDARY && mode == BrushMode.FLATTEN) {
            heightSample = brushPos.y;
            return;
        }

        // only act if mouse has been moved
        if(lastMousePosIndicator == Gdx.input.getX() + Gdx.input.getY()) return;

        // Paint
        if(mode == BrushMode.PAINT) {
            SplatMap sm = terrain.getTerrainTexture().getSplatmap();
            if(sm != null) {
                final float splatX = ((brushPos.x - terrain.getPosition().x) / (float) terrain.terrainWidth) * sm.getWidth();
                final float splatY = ((brushPos.z - terrain.getPosition().z) / (float) terrain.terrainDepth) * sm.getHeight();
                final float splatRad = (radius / terrain.terrainWidth) * sm.getWidth();
                sm.drawCircle((int) splatX, (int) splatY, (int) splatRad, strength, paintChannel);
                sm.updateTexture();
            }
            return;
        }

        final Vector3 terPos = terrain.getPosition();
        //float dir = (action == BrushAction.PRIMARY) ? 1 : -1;

        for (int x = 0; x < terrain.vertexResolution; x++) {
            for (int z = 0; z <  terrain.vertexResolution; z++) {
                final Vector3 vertexPos = terrain.getVertexPosition(tVec0, x, z);
                vertexPos.x += terPos.x;
                vertexPos.z += terPos.z;
                float distance = vertexPos.dst(brushPos);

                if(distance <= radius) {
                    final int heightIndex = z * terrain.vertexResolution + x;
                    // Raise/Lower
                    if(mode == BrushMode.RAISE_LOWER) {

                        float elevation = getValueOfBrushPixmap(brushPos, vertexPos.x, vertexPos.z);
                        terrain.heightData[heightIndex] += elevation;
                        // Flatten
                    } else if(mode == BrushMode.FLATTEN) {
                        terrain.heightData[heightIndex] = heightSample;
                    }
                }
            }
        }

        if(mode == BrushMode.RAISE_LOWER || mode == BrushMode.FLATTEN || mode == BrushMode.SMOOTH) {
            terrain.update();
        }
    }

    private float getValueOfBrushPixmap(Vector3 brushPosition, float vertexX, float vertexZ) {
        c.set(brushPosition.x, brushPosition.z);
        p.set(vertexX, vertexZ);
        v = p.sub(c);

        final float progress = v.len() / radius;
        v.nor().scl(pixmapCenter * progress);

        final float mapX = pixmapCenter + (int) v.x;
        final float mapY = pixmapCenter + (int) v.y;
        c0.set(brushPixmap.getPixel((int) mapX, (int) mapY));

        return c0.r;
    }

    public void scale(float amount) {
        sphereModelInstance.transform.scl(amount);
        radius = (boundingBox.getWidth()*sphereModelInstance.transform.getScaleX()) / 2f;
    }

    public BrushMode getMode() {
        return mode;
    }

    public void setMode(BrushMode mode) throws ModeNotSupportedException {
        if(!supportsMode(mode)) {
            throw new ModeNotSupportedException(getName() + " does not support " + mode);
        }
        this.mode = mode;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public void setTerrain(Terrain terrain) {
        this.terrain = terrain;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setPaintChannel(SplatTexture.Channel paintChannel) {
        this.paintChannel = paintChannel;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public boolean supportsMode(BrushMode mode) {
        switch (mode) {
            case RAISE_LOWER:
            case FLATTEN:
            case PAINT: return true;
        }

        return false;
    }

    @Override
    public void render() {
        if(terrain.isOnTerrain(brushPos.x, brushPos.z)) {
            batch.begin(projectContext.currScene.cam);
            batch.render(sphereModelInstance, shader);
            batch.end();
        }
    }

    @Override
    public void dispose() {
        brushPixmap.dispose();
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if(terrain != null) {
            Ray ray = projectContext.currScene.cam.getPickRay(screenX, screenY);
            terrain.getRayIntersection(brushPos, ray);
        }

        lastMousePosIndicator = screenX + screenY;
        sphereModelInstance.transform.setTranslation(brushPos);

        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if(amount < 0) {
            scale(0.9f);
        } else {
            scale(1.1f);
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return mouseMoved(screenX, screenY);
    }

}
