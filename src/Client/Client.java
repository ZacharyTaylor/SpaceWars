package Client;

import common.Debris;
import common.Missile;
import common.Galaxy;
import common.SpaceMass;
import common.Spacecraft;
import common.Star;
import com.threed.jpct.*;
import com.threed.jpct.util.KeyMapper;
import java.awt.Canvas;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.vecmath.Point2d;

/**
 * This class acts as a display for the user showing the galaxy their ship is
 * currently in. It also holds a listener to detect user actions but performs
 * no actions on the game itself.
 * @author kjb146 and zjt14
 */
public class Client extends JFrame {

    //the lighting in the galaxy, 255 = everything white, 0 = everything black
    private static final int LIGHT_LEVEL = 30;
    //the height of the camera above the galaxy
    private static final int CAMERA_HEIGHT = 800;
    //the size of the gameover message
    private static final int GAMEOVER_SIZE = 500;
    //the size of the icons and by exstension the height of the bars showing
    //player stats
    private static final int ICON_SIZE = 40;
    
    //Holds references to all models, lights and cameras
    private World world = new World();
    //buffers the image before drawing
    private FrameBuffer buffer = null;
    //the canvas onto which the game is rendered
    private Canvas glCanvas = null;
    //A key listener for detecting key presses
    private KeyMapper keyMapper;
    //world size
    private int width = Galaxy.SIZE.width;
    private int height = Galaxy.SIZE.height;
    //models used by the game
    private Object3D shipSprite = null;
    private Object3D playerSprite = null;
    private Object3D starSprite = null;
    private Object3D missileSprite = null;
    private Object3D shieldSprite = null;
    
    //hashmaps for linking the spacemasses to their corrosponding models
    private Map<Integer, Object3D> spriteCache = new HashMap<Integer, Object3D>();
    
    //hashmap for linking spacecraft to their shields
    private Map<Integer, Object3D> shieldCache = new HashMap<Integer, Object3D>();

    /**
     * Overrides normal setVisible to ensure games canvas has focus when it
     * appears so the key listener will work
     * @param b true shows the window false hides it
     */
   // @Override
   public void setVisible(boolean b) {
        super.setVisible(b);
        if (b) {
            glCanvas.requestFocus();
        }
    }

    /**
     * Constructor for the client. Sets up the game engine and loads the models
     * and textures
     */
    public Client() {

        //sets the close butten to exit the program
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        //sets output of 3D engine to errors only 
        Logger.setLogLevel(Logger.LL_ONLY_ERRORS);;

        //sets the games size
        setSize(width, height);

        //3D engine setup
        Config.tuneForIndoor();
        Config.maxPolysVisible = 10000;
        Config.autoBuild = true;
        Config.lightMul = 100;
        Config.glColorDepth = 24;
        Config.glFullscreen = false;

        // create a new buffer to render the world on:
        buffer = new FrameBuffer(width, height, FrameBuffer.SAMPLINGMODE_GL_AA_4X);
        buffer.disableRenderer(IRenderer.RENDERER_SOFTWARE);
        buffer.optimizeBufferAccess();

        //sets rendering to the canvas and adds it to client
        glCanvas = buffer.enableGLCanvasRenderer();
        add(glCanvas);

        //sets the keymapper to listen to the canvas
        keyMapper = new KeyMapper(glCanvas);

        //loads the models and textures to be used
        loadModelsAndTextures();

        //sets the lighting and camera position
        setupWorld();
    }

    /**
     * getter function for the keyMapper
     * @return keyMapper used for getting keyboard input
     */
    public KeyMapper getKeyMapper() {
        return keyMapper;
    }

    /**
     * loads the required models and textures
     */
    private void loadModelsAndTextures() {

        //gets reference to the singleton texture manager
        TextureManager tm = TextureManager.getInstance();

        //loads the shield
        shieldSprite = Primitives.getSphere(10, 40);
        shieldSprite.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
        shieldSprite.setTransparency(1);
        shieldSprite.compileAndStrip();

        //loads the sun
        tm.addTexture("sun",
                new Texture("textures" + File.separatorChar + "sun.png"));
        starSprite = Primitives.getSphere(10, 40);
        starSprite.setEnvmapped(Object3D.ENVMAP_ENABLED);
        starSprite.setTexture("sun");
        starSprite.compileAndStrip();

        //loads the enemy ship
        tm.addTexture("COLOR_0.JPG",
                new Texture("textures" + File.separatorChar + "color_0.jpg"));
        Object3D[] loader = Loader.load3DS(
                "models" + File.separatorChar + "space_frigate_0.3DS", 1.0f);
        shipSprite = Object3D.mergeAll(loader);
        shipSprite.scale(2);
        shipSprite.rotateZ((float) Math.PI);
        shipSprite.rotateMesh();
        shipSprite.setRotationMatrix(new Matrix());
        shipSprite.setOrigin(new SimpleVector(0, 0, 0));
        shipSprite.compileAndStrip();

        //loads the players ship
        tm.addTexture("FIGHTER_.PSD",
                new Texture("textures" + File.separatorChar + "fighter_1.jpg"));
        loader = Loader.load3DS(
                "models" + File.separatorChar + "fighter_1.3DS", 1.0f);
        playerSprite = Object3D.mergeAll(loader);
        playerSprite.scale(1.5f);
        playerSprite.rotateZ((float) Math.PI);
        playerSprite.rotateMesh();
        playerSprite.setRotationMatrix(new Matrix());
        //4 due to 4 units off center on the y axis
        playerSprite.setOrigin(new SimpleVector(0, 4, 0));
        playerSprite.compileAndStrip();

        //loads the missile
        missileSprite = Primitives.getCone(3, 1.2f, 8);
        missileSprite.rotateZ((float) -Math.PI / 2f);
        missileSprite.rotateMesh();
        missileSprite.setRotationMatrix(new Matrix());
        missileSprite.compileAndStrip();

        //loads the background textures
        tm.addTexture("background0",
                new Texture("textures" + File.separatorChar + "space0.png"));
        tm.addTexture("background1",
                new Texture("textures" + File.separatorChar + "space1.jpg"));
        tm.addTexture("background2",
                new Texture("textures" + File.separatorChar + "space2.png"));
        tm.addTexture("background3",
                new Texture("textures" + File.separatorChar + "space3.png"));
        tm.addTexture("background4",
                new Texture("textures" + File.separatorChar + "space4.jpg"));
        tm.addTexture("background5",
                new Texture("textures" + File.separatorChar + "space5.jpg"));
        tm.addTexture("background6",
                new Texture("textures" + File.separatorChar + "space6.jpg"));

        //loads the forground textures
        tm.addTexture("missileTex",
                new Texture("textures" + File.separatorChar + "MissilePod.jpg"));
        tm.addTexture("shieldTex",
                new Texture("textures" + File.separatorChar + "shield.jpg"));
        tm.addTexture("fuelTex",
                new Texture("textures" + File.separatorChar + "fuel.jpg"));
        tm.addTexture("box",
                new Texture("textures" + File.separatorChar + "box.jpg"));
        tm.addTexture("dead",
                new Texture("textures" + File.separatorChar + "gameover.jpg"));
        tm.addTexture("bang",
                new Texture("textures" + File.separatorChar + "bang.jpg"));
    }

    /**
     * Sets the lighting and camera position for the scene
     */
    private void setupWorld() {

        //sets up lighting
        world.getLights().setOverbrightLighting(
                Lights.OVERBRIGHT_LIGHTING_DISABLED);
        world.getLights().setRGBScale(Lights.RGB_SCALE_4X);

        // Set the overall brightness of the world:
        world.setAmbientLight(LIGHT_LEVEL, LIGHT_LEVEL, LIGHT_LEVEL);

        //sets up game view
        Camera camera = world.getCamera();
        camera.setPosition(width / 2, height / 2, CAMERA_HEIGHT);
        camera.lookAt(SimpleVector.create(width / 2, height / 2, 0));
    }

    /**
     * update the display
     * @param galaxy the galaxy to display
     */
    public void update(Galaxy galaxy) {

        //the players ship
        Spacecraft playersShip = null;

        //sets the stars' color
        starSprite.setAdditionalColor(galaxy.getStarColor());

        //sets the background
        setBackground(galaxy.getBackNum());

        //used to determine which sprites are no longer required
        Collection<Integer> cacheCleaner = 
                new ArrayList<Integer>(spriteCache.keySet());

        //adds all the spacemasses to the buffer
        for (SpaceMass sm : galaxy.getAllSpaceMasses()) {

            //removes from cleaner as still present in galaxy
            cacheCleaner.remove(sm.id);

            //adds or moves sprites as nessecery
            updateSprite(sm, galaxy.getPlayerID());

            //assigns correct ship to player
            if (sm.id == galaxy.getPlayerID()) {
                playersShip = ((Spacecraft) sm);
            }

        }

        //Remove sprites from the cache which are no longer objects
        for (Integer i : cacheCleaner) {
            
            //the sprite to be removed
            Object3D tempSprite = spriteCache.get(i);
            //its assosuated shield (if it has one)
            Object3D tempShield = shieldCache.get(i);
            
            //remove shield
            if(tempShield != null){
                world.removeObject(tempShield);
                shieldCache.remove(i);
            }

            //remove sprite
            world.removeObject(tempSprite);
            spriteCache.remove(i);
        }

        //render and display galaxy
        displayGalaxy(playersShip);
    }

    /**
     * adds color to the shield to allow viewing of enemy strength
     * 5 = white (no effect)
     * 4 = blue
     * 3 = green
     * 2 = yellow
     * 1 = red
     * @param strength the Spacecrafts shield strength
     * @param shield the shield to color
     */
    private void shieldColor(int strength, Object3D shield){
        switch (strength) {
                case 1:
                    shield.setAdditionalColor(Color.red);
                    break;
                case 2:
                    shield.setAdditionalColor(Color.yellow);
                    break;
                case 3:
                    shield.setAdditionalColor(Color.green);
                    break;
                case 4:
                    shield.setAdditionalColor(Color.blue);
                    break;
                default:
                    break;
            }
    }
    
    /**
     * adds a shield to a spacecraft
     * @param sc the spacecraft to add a shield to
     * @param parent the Spacecraft's 3d object to bind the shield to
     */
    private void addShield(Spacecraft sc, Object3D parent) {
        
        //ensure a shield is needed
        if (sc.getShields() >= 0) {
            //create the shield
            Object3D shield = shieldSprite.cloneObject();
            shieldColor(sc.getShields(), shield);
            shield.addParent(parent);
            
            //add it to the world
            shieldCache.put(sc.id, shield);
            world.addObject(shield);
        }
    }

    /**
     * Adds a new sprite or if one is already present updates its position
     * @param sm the SpaceMass which needs a sprite
     * @param galaxy the current galaxy
     */
    private void updateSprite(SpaceMass sm, int playerID) {

        //if the spacemass already exists retreive it
        Object3D sprite = spriteCache.get(sm.id);

        //the spacemass is already present
        if (sprite != null) {

            //clears previous location
            sprite.clearTranslation();
            sprite.clearRotation();

        } 
        //the spacemass isnt present so create a new instance
        else {
            sprite = addSprite(sm, playerID);
        }

        //move to correct position and orientation
        sprite.translate(
                new SimpleVector(sm.getLocation().x, sm.getLocation().y, 0));
        sprite.rotateZ(
                -(float) sm.getHeading());

        //if its debris give it some 3d tumble and an explosion
        if (sm instanceof Debris) {
            debrisEffects(sprite, (Debris)sm);
        }   
        
        Object3D shield = shieldCache.get(sm.id);
        if(shield != null){
            shieldColor(((Spacecraft)sm).getShields(),shield);
        }
    }
    
    /**
     * Adds a sprite to the world
     * @param sm the SpaceMass whose sprite is to be added
     * @param playerID the id of the players ship
     * @return sprite the sprite to add
     */
    private Object3D addSprite(SpaceMass sm, int playerID){
            
            Object3D sprite = null;
        
            //create a star
            if (sm instanceof Star) {

                sprite = starSprite.cloneObject();
                sprite.setMesh(Primitives.getSphere(10, sm.getRadius()).getMesh());

            } 
            
            //create a missile
            else if (sm instanceof Missile) {
                sprite = missileSprite.cloneObject();
            } 
            
            //create a spaceship
            else if (sm instanceof Spacecraft) {
                
                //create appropriate ship
                if (sm.id == playerID) {
                    sprite = playerSprite.cloneObject();
                } else {
                    sprite = shipSprite.cloneObject();
                }
                //give it a shield
                addShield((Spacecraft) sm, sprite);  
            } 
            
            //creat debris
            else if (sm instanceof Debris) {
                
                //create appropriate debris
                if (((Debris) sm).getDebrisID() == playerID) {
                    sprite = playerSprite.cloneObject();
                } else {
                    sprite = shipSprite.cloneObject();
                }

                //cause debris to explode and spin
                debrisEffects(sprite, (Debris)sm);
            }
            
            //add to the cache and world
            spriteCache.put(sm.id, sprite);
            world.addObject(sprite);
            
            return sprite;
    }
    
    /**
     * Adds 3d tumbling and explosions to debris
     * @param sprite the debris sprite
     * @param d the debris
     */
    private void debrisEffects(Object3D sprite, Debris d) {
        sprite.rotateX(d.getTumble());

        int frame = d.getExplosionFrame();
        if (frame <= Debris.FRAMES_IN_EXPLOSION) {
            addExplosion(frame, Debris.EXPLOSION_SIZE, d.getLocation());
        }
    }

    /**
     * Draws the galaxy and the player information to the screen
     * @param playersShip the players Spacecraft (null if player is dead)
     */
    private void displayGalaxy(Spacecraft playersShip) {
        // render the world onto the buffer:
        world.renderScene(buffer);
        world.draw(buffer);

        // if player dead display game over
        if (playersShip == null) {
            TextureManager tm = TextureManager.getInstance();

            setIcon(tm.getTexture("dead"),
                    GAMEOVER_SIZE,
                    new Point2d((width - GAMEOVER_SIZE) / 2, 0),
                    true);
        } 
        //else show stats
        else {
            drawPlayerStats(playersShip);
        }

        buffer.update();
        buffer.displayGLOnly();
        glCanvas.repaint();
    }

    /**
     * Adds an explosion to the buffer for display
     * @param frame the frame of the explosion animation to show
     * @param size the size of the explosion
     * @param offset the location of the explosion from the top right of the
     * screen
     */
    private void addExplosion(int frame, Point2d size, Point2d offset) {

        //checks to ensure frame is valid
        if (frame < Debris.FRAMES_IN_EXPLOSION) {

            //the width and height of the grid of frames
            final int FRAME_POS = (int) Math.sqrt(Debris.FRAMES_IN_EXPLOSION);

            //gets the explosion texture
            TextureManager tm = TextureManager.getInstance();
            Texture bang = tm.getTexture("bang");

            //adds the desured frame of the explosion to the buffer
            buffer.blit(bang,
                    (bang.getHeight() / FRAME_POS) * ((int) (frame % FRAME_POS)),
                    (bang.getWidth() / FRAME_POS) * ((int) (frame / FRAME_POS)),
                    (int) (width - offset.x),
                    (int) (offset.y),
                    bang.getWidth() / FRAME_POS,
                    bang.getHeight() / FRAME_POS,
                    (int) size.x,
                    (int) size.y,
                    0,
                    false,
                    null);
        }
    }

    /**
     * draws bars used for displaying player stats onto the screen
     * @param number the number of bars
     * @param size the size of each bar
     * @param offset the distance from the top left of the screen to the top
     *               left of the left most bar
     * @param color  the color of the bars
     */
    private void setBars(int number, Point2d size, Point2d offset, Color color) {

        //get instance of tekture manager
        TextureManager tm = TextureManager.getInstance();
        //gets the bar texture out of it
        Texture bar = tm.getTexture("box");

        //for each bar
        for (int i = 0; i < number; i++) {
            //add to buffer
            buffer.blit(bar,
                    0,
                    0,
                    ((int) size.x + 1) * i + (int) offset.x,
                    (int) offset.y,
                    bar.getWidth(),
                    bar.getHeight(),
                    (int) size.x,
                    (int) size.y,
                    -1,
                    false,
                    color);
        }
    }

    /**
     * Adds a square icon to the buffer
     * @param icon the texture to use for the icon
     * @param iconSize the icons width and height (icon must be square)
     * @param offset the distance from the top left of the screen to the top
     *               left of the icon
     * @param noBlack if true black in image rendered as see through. 
     *               opaque if false
     */
    private void setIcon(Texture icon, int iconSize, Point2d offset, boolean noBlack) {

        //converts variable noBlack to the less intuitve integer form required
        //by the buffer
        int noBlackInt;
        if (noBlack) {
            noBlackInt = 0;
        } else {
            noBlackInt = -1;
        }
        //adds icon to buffer
        buffer.blit(icon,
                0,
                0,
                (int) offset.x,
                (int) offset.y,
                icon.getWidth(),
                icon.getHeight(),
                iconSize,
                iconSize,
                noBlackInt,
                false,
                null);
    }

    /**
     * draws the players stats to the screen
     * @param player the player whose stats are being drawn
     */
    private void drawPlayerStats(Spacecraft player) {

        //get an instance of the texture manager
        TextureManager tm = TextureManager.getInstance();

        //add overtop of other images
        buffer.setBlittingTarget(FrameBuffer.BLITTING_TARGET_FRONT);

        //add shields icon
        setIcon(tm.getTexture("shieldTex"),
                ICON_SIZE,
                new Point2d(0, height - ICON_SIZE * 3),
                false);

        //add shields counter
        setBars(player.getShields(),
                new Point2d(ICON_SIZE / 2, ICON_SIZE),
                new Point2d(ICON_SIZE, height - ICON_SIZE * 3),
                Color.green);

        //add missiles icon
        setIcon(tm.getTexture("missileTex"),
                ICON_SIZE,
                new Point2d(0, height - ICON_SIZE * 2),
                false);

        //add missiles counter
        setBars(player.getMissilesLeft(),
                new Point2d(ICON_SIZE / 20, ICON_SIZE),
                new Point2d(ICON_SIZE, height - ICON_SIZE * 2),
                Color.white);

        //add fuel icon
        setIcon(tm.getTexture("fuelTex"),
                ICON_SIZE,
                new Point2d(0, height - ICON_SIZE * 1),
                false);

        //add fuel counter
        setBars(player.getFuel() / 10,
                new Point2d(ICON_SIZE / 40, ICON_SIZE),
                new Point2d(ICON_SIZE, height - ICON_SIZE * 1),
                Color.yellow);
    }

    /**
     * adds the background to the buffer
     * @param backNum number used to choose background
     */
    private void setBackground(int backNum) {

        //gets an instance of the texture manager
        TextureManager tm = TextureManager.getInstance();

        // erase the previous frame
        buffer.clear();

        //draw behind other objects
        buffer.setBlittingTarget(FrameBuffer.BLITTING_TARGET_BACK);

        //selects a random background
        Texture background = tm.getTexture("background"
                + Integer.toString(backNum));

        //adds  background to the buffer
        buffer.blit(background,
                0,
                0,
                0,
                0,
                background.getWidth(),
                background.getHeight(),
                FrameBuffer.OPAQUE_BLITTING);
    }
}
