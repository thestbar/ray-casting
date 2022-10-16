package com.thestbar.raycasting;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class RayCasting extends Game {
    protected PolygonSpriteBatch batch;
    private Texture whitePixel;
	protected TextureRegion region;
	protected ShapeDrawer drawer;
	protected BitmapFont font;
	protected Skin skin;
	
	@Override
	public void create () {
		// Initialize sprite batch
		batch = new PolygonSpriteBatch();

		// Initialize texture region used in shape drawer
		// this texture region is just a white pixel, so it can
		// be easily colored. Instead of using a real texture
		// we can create one using code
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.drawPixel(0, 0);
		whitePixel = new Texture(pixmap); //remember to dispose of later
		pixmap.dispose();
		region = new TextureRegion(whitePixel, 0, 0, 1, 1);

		// ShapeDrawer needs a batch and a texture region to work
		drawer = new ShapeDrawer(batch, region);

		// Initialize font for labels. Using default LibGDX arial font
		font = new BitmapFont();
		font.setColor(Color.WHITE);
		font.getData().setScale(10);

		// Initialize skin item
		skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

		this.setScreen(new MainScreen(this));
	}

	@Override
	public void render () {
		super.render();
	}

	@Override
	public void dispose () {
		whitePixel.dispose();
		font.dispose();
		batch.dispose();
		skin.dispose();
	}
}
