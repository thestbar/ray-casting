package com.thestbar.raycasting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.thestbar.raycasting.util.CenteredRectangle;

import java.io.IOException;

public class GameScreen implements Screen {
    private final RayCasting game;
    private OrthographicCamera camera;
    private Vector2 player = new Vector2(300, 40);
    private Vector2 playerDir = new Vector2(1, 0);
    private float playerMovementSpeed = 100;
    private float playerRotationMovementSpeed = 150;
    private Vector2 mapSize = new Vector2(24, 24);
    private Vector2 cellSize = new Vector2(30, 30);
    private int[] map = new int[(int)(mapSize.x * mapSize.y)];
    private Vector2 mouse;
    private boolean isDrawingRayIntersections;
    private final int NUM_OF_RAYS = Gdx.graphics.getWidth() / 2;
    private final float SLOW_RAY_CASTER_DELTA_DISTANCE = 1f;
    private final float FOV = 50;
    private float fpsCounterInterval = 0;
    private final float UPDATE_FPS_INTERVAL = 1;
    private final String LEVEL_MAP_PATH = "./assets/levelMaps/Level2_Map.txt"; // Contains the Path to current level
    private final int NUMBER_OF_TEXTURES = 11;
    private Texture[] textures;
    private final int TEXTURE_WIDTH = 64;
    private final int TEXTURE_HEIGHT = 64;
    // If 0 then DDA Algorithm is used
    // If 1 then Slow Algorithm is used
    private int rayCaster = 0;

    public GameScreen(RayCasting game) throws IOException {
        this.game = game;

        // Load textures
        loadTextures();

        // Create camera
        camera = new OrthographicCamera();

        // This fixes the problem were the input handling system counts
        // from top left when the draw is happening from bottom left
        // While rendering need to set projection matrix of sprite batch
        // to the matrix of the camera!
        camera.setToOrtho(true);

        // initialize player settings
        mouse = new Vector2();
        isDrawingRayIntersections = false;

        // Initialize map
        initializeMap();
    }

    @Override
    public void show() {

    }

    private void input(float deltaTime) {
        mouse = new Vector2(Gdx.input.getX(), Gdx.input.getY());

        Vector2 cell = new Vector2((float)Math.floor(mouse.x / cellSize.x),
                (float)Math.floor(mouse.y / cellSize.y));

        // Paint with right mouse button "solid" tiles
        if(Gdx.input.isTouched()) map[(int)(cell.y * mapSize.x + cell.x)] = 1;

        // Get a copy of player's current position
        Vector2 playerCpy = player.cpy();

        // Move "player" position
        if(Gdx.input.isKeyPressed(Input.Keys.W)) {
            playerCpy.x += playerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += playerDir.y * playerMovementSpeed * deltaTime;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.S)) {
            Vector2 behindFromPlayerDir = playerDir.cpy().rotateDeg(180);
            playerCpy.x += behindFromPlayerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += behindFromPlayerDir.y * playerMovementSpeed * deltaTime;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.A)) {
            playerDir.rotateDeg(-deltaTime * playerRotationMovementSpeed);
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.D)) {
            playerDir.rotateDeg(deltaTime * playerRotationMovementSpeed);
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            Vector2 leftFromPlayerDir = playerDir.cpy().rotateDeg(-90);
            playerCpy.x += leftFromPlayerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += leftFromPlayerDir.y * playerMovementSpeed * deltaTime;
        }
        else if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            Vector2 rightFromPlayerDir = playerDir.cpy().rotateDeg(90);
            playerCpy.x += rightFromPlayerDir.x * playerMovementSpeed * deltaTime;
            playerCpy.y += rightFromPlayerDir.y * playerMovementSpeed * deltaTime;
        }

        detectCollisions(playerCpy);

        // If space is pressed then enable/disable drawing intersection of rays with wall
        if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            isDrawingRayIntersections = !isDrawingRayIntersections;

        // If R button is pressed change ray caster in use
        if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            if(rayCaster == 0) rayCaster = 1;
            else rayCaster = 0;
        }

    }

    @Override
    public void render(float delta) {
        // Set FPS counter in Desktop title
        countFps(delta);

        ScreenUtils.clear(0, 0, 0, 1);

        game.batch.setProjectionMatrix(camera.combined);

        input(delta);

        // Draw Map
        drawMap2D();

        // Draw player
        drawPlayer2D();

        // Draw mouse
        drawMouse2D();

        // Cast rays
        // When you know the origin point the length of the line and the direction
        // you can find the position of the point of the line by the formula below
        // (x, y) = (x1 + a * l, y1 + b * l)
        // Where l is the length of the line
        // x1, y1 are the coordinates of the starting point
        // a, b are the coordinates of the direction vector
        Vector2 currRayDir = playerDir.cpy().rotateDeg(-FOV / 2);
        final float rayStep = FOV / NUM_OF_RAYS;
        for(int i = 0; i < NUM_OF_RAYS; i++) {
            // Selection between 2 different ray casters
            if(rayCaster == 0)
                castRayDDAAlgo(player, currRayDir, i, Math.abs(currRayDir.angleDeg() - playerDir.angleDeg()));
            else if(rayCaster == 1)
                castRaySlowAlgo(player, currRayDir, i, Math.abs(currRayDir.angleDeg() - playerDir.angleDeg()));

            currRayDir.rotateDeg(rayStep);
        }
    }

    void countFps(float delta) {
        int fps = (int)(1 / delta);
        if(fpsCounterInterval >= UPDATE_FPS_INTERVAL) {
            Gdx.graphics.setTitle("RayCasting - FPS: " + fps);
            fpsCounterInterval = 0;
        }
        else {
            fpsCounterInterval += delta;
        }
    }

    void castRayDDAAlgo(Vector2 sPos, Vector2 rayDir, int rayIndex, float angleBetweenCameraAndRay) {
        Vector2 startPos = sPos.cpy();

        // Find angle from direction vector
        float angle = (float)(Math.atan2(rayDir.y, rayDir.x));

        // Find the tile on which the player is
        // Using this vector find the grid position of the ray
        int posX = (int)(startPos.x / cellSize.x);
        int posY = (int)(startPos.y / cellSize.y);

        // If it goes out of screen for X or Y axis stop going further
        if(posX < 0) posX = 0; if(posX > mapSize.x - 1) posX = (int)mapSize.x - 1;
        if(posY < 0) posY = 0; if(posY > mapSize.y - 1) posY = (int)mapSize.y - 1;

        // Create variables that will hold the value of the length from player till
        // the current position of the ray for both travelling on X and Y axis
        float currLenDeltaX;
        float currLenDeltaY;

        // Calculate the step on the length when moving on X and Y axis
        float stepDeltaX = (float)Math.abs(cellSize.x / Math.cos(angle));
        float stepDeltaY = (float)Math.abs(cellSize.y / Math.sin(angle));

        // Calculate the initial values (before starting the actual DDA)
        // If ray's direction vector x value is > 0 then this means
        // that when travelling on X axis the ray will go to the right
        // If it is < 0 then it will go to the left
        if(rayDir.x > 0) {
            float rightBarrier = cellSize.x * (posX + 1);
            float deltaX = rightBarrier - startPos.x;
            currLenDeltaX = deltaX / (float)Math.cos(angle);
        }
        else {
            float leftBarrier = cellSize.x * posX;
            float deltaX = leftBarrier - startPos.x;
            currLenDeltaX = deltaX / (float)Math.cos(angle);
        }

        // Similar way when rayDir.y > 0 then the ray will move down
        // and when rayDir.y < 0 then the ray will move up
        if(rayDir.y > 0) {
            float downBarrier = cellSize.y * (posY + 1);
            float deltaY = downBarrier - startPos.y;
            currLenDeltaY = deltaY / (float)Math.sin(angle);
        }
        else {
            float topBarrier = cellSize.y * posY;
            float deltaY = topBarrier - startPos.y;
            currLenDeltaY = deltaY / (float)Math.sin(angle);
        }

        // Boolean variable that indicates that the ray hit a wall
        boolean hit = false;

        // Boolean variables that indicates if the hit was made
        // on an X or Y axis wall
        boolean movedOnXAxis;

        // Vector that will hold the intersection between wall and ray
        Vector2 rayEndPos = new Vector2();

        // Perpendicular ray distance from player is stored here
        float perpRayDistance = 0;
        
        // Euclidean ray distance from player is stored here
        float euclRayDistance = 0;

        // Boolean variable that knows if the ray hit a wall on X or Y axis
        boolean rayHitSideValue = false;

        // Stores the value of the wall that was hit
        int rayValue = 0;

        // Holds the value of the distance of the ray's position from the edge of cell
        float rayTexturePosition = 0;

        // Actual DDA starts here
        while(!hit) {
            // Always select to move on the direction which has the smallest length
            if(currLenDeltaX < currLenDeltaY) {
                // Move delta X
                currLenDeltaX += stepDeltaX;
                if(rayDir.x > 0) posX++;
                else posX--;
                movedOnXAxis = true;
            }
            else {
                // Move delta Y
                currLenDeltaY += stepDeltaY;
                if(rayDir.y > 0) posY++;
                else posY--;
                movedOnXAxis = false;
            }

            // Check for collisions with walls
            int currCellPos = (int)(posY * mapSize.x + posX);

            if(map[currCellPos] > 0) {
                hit = true;
                if(movedOnXAxis) {
                    // Calculate intersection based on currLenDeltaX
                    rayEndPos = startPos.cpy().add(rayDir.cpy().scl(currLenDeltaX - stepDeltaX));
                    euclRayDistance = (currLenDeltaX - stepDeltaX);
                    perpRayDistance = euclRayDistance *
                            (float)Math.cos(Math.toRadians(angleBetweenCameraAndRay));
                    rayHitSideValue = true;
                }
                else {
                    // Calculate intersection based on currLenDeltaY
                    rayEndPos = startPos.cpy().add(rayDir.cpy().scl(currLenDeltaY - stepDeltaY));
                    euclRayDistance = (currLenDeltaY - stepDeltaY);
                    perpRayDistance = euclRayDistance *
                            (float)Math.cos(Math.toRadians(angleBetweenCameraAndRay));
                }

                // Save the type of the wall that was hit
                rayValue = map[(int)(posY * mapSize.x + posX)];
            }
        }

        // Currently this check is useless because always it hit a wall
        if(hit) {
            // Draw line
            game.batch.begin();
            game.drawer.setColor(Color.WHITE);
            game.drawer.line(startPos, rayEndPos);
            game.batch.end();
        }

        // Draw yellow circles if space bar is clicked
        if(isDrawingRayIntersections && hit) {
            game.batch.begin();
            game.drawer.setColor(Color.YELLOW);
            game.drawer.circle(rayEndPos.x, rayEndPos.y, cellSize.x / 4);
            game.batch.end();
        }

        // Draw 3D screen
        drawScreen3D(rayHitSideValue, perpRayDistance, rayIndex, rayValue, euclRayDistance, rayDir, posX, posY);

    }

    void castRaySlowAlgo(Vector2 sPos, Vector2 rayDir, int index, float angleBetweenCameraAndRay) {
        // Index represents the index of
        // the current ray that is calculated

        Vector2 startPos = new Vector2(sPos);
        Vector2 dir = new Vector2(rayDir);

        // Find angle from direction vector
        float angle = (float)(Math.atan2(dir.y, dir.x));

        // Get the slope of the line that connects the starting
        // and the ending position of the ray
        // Line for given x, then y = slope * (x - x0) + y0
        // where (x0, y0) can be start or end position
        float slope = dir.y / dir.x;

        // For this delta distance, calculate
        // delta movement on X axis
        float deltaX = (float)Math.cos(angle) * SLOW_RAY_CASTER_DELTA_DISTANCE;

        // Perform the checks
        int i = 0;

        Vector2 intersection = new Vector2();

        boolean hitWall = false;

        // Holds the perpendicular distance of the ray from the camera pane
        float perpRayDistance = 0;

        // Holds the euclidean distance of the ray from the camera pane
        float euclRayDistance = 0;

        // Grid current position of the ray
        int gridX = 0;
        int gridY = 0;

        // Holds the value of the cell that was hit by the ray (and was a wall)
        int rayValue = 0;

        while(!hitWall) {
            // On each step find the current x position of the ray
            float currX = startPos.x + deltaX * i;

            // Using the line's coordinates constructor
            // find the current y position of the ray
            float currY = slope * (currX - startPos.x) + startPos.y;

            // Create a vector for the current position
            Vector2 point = new Vector2(currX, currY);

            // Using this vector to find the grid position of the ray
            gridX = (int)(point.x / cellSize.x);
            gridY = (int)(point.y / cellSize.y);

            // If it goes out of screen for X or Y axis stop going further
            if(gridX < 0) gridX = 0; if(gridX > mapSize.x - 1) gridX = (int)mapSize.x - 1;
            if(gridY < 0) gridY = 0; if(gridY > mapSize.y - 1) gridY = (int)mapSize.y - 1;

            int cellPos = (int)(gridY * mapSize.x + gridX);
            if(cellPos >= 0 && cellPos <= mapSize.x * mapSize.y - 1) {
                int value = map[cellPos];
                // If value is 1 then ray hit a wall
                if(value > 0) {
                    hitWall = true;
                    intersection = point;
                    // Save the map value of the current intersection
                    rayValue = map[cellPos];
                }
                else {
                    intersection = point;
                }
                i++;
            }
        }

        // We know that ray hit cell at [gridX, gridY];
        // Find the borders of this cell and check if the intersection is
        // on up or down border which means that it did not hit a side
        float pixDelta = 1;
        float upBorder = cellSize.y * gridY;
        float downBorder = cellSize.y * (gridY + 1);

        boolean rayHitSideValue = (!(intersection.y > upBorder - pixDelta) || !(intersection.y < upBorder + pixDelta)) &&
                (!(intersection.y > downBorder - pixDelta) || !(intersection.y < downBorder + pixDelta));


        // Store the distance between the starting position
        // and the intersection on the specific index in the array
        // In order to fix fish-eye-effect instead of using
        // the euclidean distance we have to multiply it
        // by the cosine of the angle between the current ray
        // and the direction of the camera
        // Also store the euclidean distance
        euclRayDistance = intersection.dst(startPos);
        perpRayDistance = euclRayDistance * (float)Math.cos(Math.toRadians(angleBetweenCameraAndRay));

        // Draw ray
        game.batch.begin();
        game.drawer.setColor(Color.WHITE);
        game.drawer.line(startPos, intersection);
        game.batch.end();

        // Draw the ray intersection if wall has been hit
        if(isDrawingRayIntersections && hitWall) {
            game.batch.begin();
            game.drawer.setColor(Color.YELLOW);
            game.drawer.circle(intersection.x, intersection.y, cellSize.x / 4);
            game.batch.end();
        }

        // Draw 3D screen
        drawScreen3D(rayHitSideValue, perpRayDistance, index, rayValue, euclRayDistance, rayDir, gridX, gridY);
    }

    void drawScreen3D(boolean rayHitSideValue, float perpRayDistance, int rayIndex, int rayValue, float euclRayDistance, Vector2 rayDir, int rayPosX, int rayPosY) {
        // Calculate the number of pixels that each column of the 3D contains
        float pixelsOfEachCol = Gdx.graphics.getWidth() / 2f / NUM_OF_RAYS;

        // If the ray hit the Y-Axis then make the wall a bit darker
        // This makes the 3D screen looking better, visually
        float alpha = (rayHitSideValue) ? 0.7f : 1;

        // Calculate the height of the column of the 3D screen
        // This is calculated by dividing the maximum height of the screen
        // by the perpendicular distance of the intersection from the camera pane
        // Also, we multiply this by a final variable, to make the walls higher
        float rectangleHeight = 30 * Gdx.graphics.getHeight() / perpRayDistance;

        // Offset on the X-Axis of the screen is calculated
        // (This applies only when both 2D and 3D worlds are drawn
        float xOffset = Gdx.graphics.getWidth() / 2f;

        // Create the centered rectangle of the wall
        Rectangle rectangle = new CenteredRectangle(xOffset + rayIndex * pixelsOfEachCol + pixelsOfEachCol / 2,
                Gdx.graphics.getHeight() / 2f, pixelsOfEachCol, rectangleHeight);

        // Create the ceiling rectangle (above the wall)
        Rectangle ceilingRectangle = new Rectangle(xOffset + rayIndex * pixelsOfEachCol,
                0, pixelsOfEachCol, (Gdx.graphics.getHeight() - rectangleHeight) / 2f);

        // Create the floor rectangle (below the wall)
        Rectangle floorRectangle = new Rectangle(xOffset + rayIndex * pixelsOfEachCol,
                rectangle.y + rectangle.height, pixelsOfEachCol, (Gdx.graphics.getHeight() - rectangleHeight) / 2f);

        // Draw the rectangles in the 3D screen for floor and ceiling
        game.batch.begin();
        game.drawer.setColor(Color.DARK_GRAY);
        game.drawer.filledRectangle(floorRectangle);
        game.drawer.setColor(Color.BROWN);
        game.drawer.filledRectangle(ceilingRectangle);
        game.batch.end();

        // Removing 1 because value 0 means that
        // we do not draw anything and value 1 is the
        // corresponding value for texture at index 0
        int textureIndex = rayValue - 1;

        // Calculate where exactly the wall was hit
        float distanceFromEdgeRatio;

        // Find intersection between ray and wall
        Vector2 intersection = player.cpy().add(rayDir.cpy().scl(euclRayDistance));

        if(rayHitSideValue) {
            // Find distance from top
            float topY = rayPosY * cellSize.y;
            distanceFromEdgeRatio = (intersection.y - topY) / cellSize.y;
        }
        else {
            // Find distance from right
            float topX = (rayPosX + 1) * cellSize.x;
            distanceFromEdgeRatio = (topX - intersection.x) / cellSize.x;
        }

        // Calculate X coordinate on the texture
        int texX = (int)(distanceFromEdgeRatio * TEXTURE_WIDTH);

        TextureRegion wallRegion = new TextureRegion(textures[textureIndex], texX, 0, Gdx.graphics.getWidth() / (2 * NUM_OF_RAYS), 64);

        game.batch.begin();
        game.batch.setColor(1, 1, 1, alpha);
        game.batch.draw(wallRegion, rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        game.batch.end();

    }

    void drawMap2D() {
        for(int y = 0; y < mapSize.y; y++) {
            for(int x = 0; x < mapSize.x; x++) {
                int cell = map[(int) (y * mapSize.x + x)];
                game.batch.begin();
                // If cell has value bigger than 0, draw a color inside it
                if(cell == 1)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.ORANGE);
                else if(cell == 2)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.RED);
                else if(cell == 3)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.PURPLE);
                else if(cell == 4)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.GRAY);
                else if(cell == 5)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.BLUE);
                else if(cell == 6)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.YELLOW);
                else if(cell == 7)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.BROWN);
                else if(cell == 8)
                    game.drawer.filledRectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.MAROON);
                // Draw cell boundary
                game.drawer.rectangle(x * cellSize.x, y * cellSize.y, cellSize.x, cellSize.y, Color.DARK_GRAY);
                game.batch.end();
            }
        }
    }

    void detectCollisions(Vector2 playerPosition) {
        // Check if new position is valid
        // Find the tile that the new position is into
        // if it is a wall do not let it move
        Vector2 playerNewPositionCell = new Vector2((float)Math.floor(playerPosition.x / cellSize.x),
                (float)Math.floor(playerPosition.y / cellSize.y));
        if(map[(int)(playerNewPositionCell.y * mapSize.x + playerNewPositionCell.x)] == 0) {
            player = playerPosition;
        }
        // If the new position is invalid
        // Check if only moving left/right is valid
        else {
            Vector2 playerPositionCpy = player.cpy();
            playerPositionCpy.x = playerPosition.x;
            // Check if position with only x added is valid
            playerNewPositionCell = new Vector2((float)Math.floor(playerPositionCpy.x / cellSize.x),
                    (float)Math.floor(playerPositionCpy.y / cellSize.y));
            if(map[(int)(playerNewPositionCell.y * mapSize.x + playerNewPositionCell.x)] <= 0) {
                player = playerPositionCpy;
            }
            // If new position is invalid and
            // moving left or right is invalid
            // Check if only moving up/down is valid
            else {
                playerPositionCpy = player.cpy();
                playerPositionCpy.y = playerPosition.y;
                // Check if position with only y added is valid
                playerNewPositionCell = new Vector2((float)Math.floor(playerPositionCpy.x / cellSize.x),
                        (float)Math.floor(playerPositionCpy.y / cellSize.y));
                if(map[(int)(playerNewPositionCell.y * mapSize.x + playerNewPositionCell.x)] == 0) {
                    player = playerPositionCpy;
                }
            }
        }
    }

    void drawPlayer2D() {
        game.batch.begin();
        game.drawer.filledCircle(player, cellSize.x / 4, Color.RED);
        game.batch.end();
    }

    void drawMouse2D() {
        game.batch.begin();
        game.drawer.filledCircle(mouse, cellSize.x / 4, Color.GREEN);
        game.batch.end();
    }

    void initializeMap() throws IOException {
        GridSetup gridSetup = new GridSetup(LEVEL_MAP_PATH, (int)mapSize.y, (int)mapSize.x);
        map = gridSetup.getGrid();
    }

    void loadTextures() {
        textures = new Texture[NUMBER_OF_TEXTURES];
        textures[0] = new Texture(Gdx.files.internal("textures/eagle.png"));
        textures[1] = new Texture(Gdx.files.internal("textures/redbrick.png"));
        textures[2] = new Texture(Gdx.files.internal("textures/purplestone.png"));
        textures[3] = new Texture(Gdx.files.internal("textures/greystone.png"));
        textures[4] = new Texture(Gdx.files.internal("textures/bluestone.png"));
        textures[5] = new Texture(Gdx.files.internal("textures/mossy.png"));
        textures[6] = new Texture(Gdx.files.internal("textures/wood.png"));
        textures[7] = new Texture(Gdx.files.internal("textures/colorstone.png"));
        textures[8] = new Texture(Gdx.files.internal("textures/pillar.png"));
        textures[9] = new Texture(Gdx.files.internal("textures/greenlight.png"));
        textures[10] = new Texture(Gdx.files.internal("textures/barrel.png"));
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        for(Texture texture: textures) {
            texture.dispose();
        }
    }
}
