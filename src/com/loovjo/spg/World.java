package com.loovjo.spg;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.UnaryOperator;

import com.loovjo.loo2D.utils.ImageLoader;
import com.loovjo.loo2D.utils.Vector;
import com.loovjo.spg.gameobject.GameObject;
import com.loovjo.spg.gameobject.Part;
import com.loovjo.spg.gameobject.parts.SpaceShipMainComputerRoom;
import com.loovjo.spg.gameobject.player.Player;
import com.loovjo.spg.gameobject.utils.CollisionLineSegment;
import com.loovjo.spg.gameobject.utils.LineSegment;
import com.loovjo.spg.gui.Gui;
import com.loovjo.spg.utils.Textures;

public class World {

	public float zoom = 80; // Pixel per unit

	public ArrayList<GameObject> objects = new ArrayList<GameObject>();

	public HashMap<Integer, UnaryOperator<World>> keyBindings = new HashMap<Integer, UnaryOperator<World>>();

	public Vector camPos = new Vector(0, 0); // In units

	public int tick;

	public Vector camVel = new Vector(0, 0);
	public Vector camAccel = new Vector(0, 0);

	public BufferedImage background;
	public float background_depth = 10;

	public boolean movingCamToPlayer = false;

	public int width, height;

	public float DEFAULT_SPREAD = 0.3f;
	public float FRICTION = 2f;

	public static float MAX_SPEED = 10;
	public static float MAX_ROT = 3;

	public Optional<Part> active = Optional.empty();

	private Gui gui = null;

	public GameScene owner;

	public World(GameScene scene) {
		owner = scene;

		try {
			background = Textures.BACKGROUND.toBufferedImage();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Player player = new Player(this, new Vector(0, 0), "Player");

		Part playerBody = new Part(new Vector(0, 0), new Vector(0, 0), player, 0, 0.7f, 1, Textures.PLAYER_BODY,
				Textures.PLAYER_BODY_COLMESH);

		Part armLeft1 = new Part(new Vector(0, -0.5), new Vector(0, -0.3), playerBody, (float) Math.PI, 0.5f, 1,
				Textures.PLAYER_ARM, Textures.PLAYER_ARM_COLMESH);
		Part armRight1 = new Part(new Vector(0, -0.5), new Vector(0, -0.3), playerBody, (float) Math.PI, 0.5f, 1,
				Textures.PLAYER_ARM, Textures.PLAYER_ARM_COLMESH);

		Part legLeft1 = new Part(new Vector(0, 0.35), new Vector(0, -0.3), playerBody, (float) Math.PI, 0.5f, 1,
				Textures.PLAYER_LEG_1, Textures.PLAYER_LEG_1_COLMESH);
		legLeft1.setRelativeZIndex(-1);

		Part legLeft2 = new Part(new Vector(0, -0.25), new Vector(0, -0.2), legLeft1, 0, 0.5f, 1, Textures.PLAYER_LEG_2,
				Textures.PLAYER_LEG_2_COLMESH);
		
		Part legRight1 = new Part(new Vector(0, 0.35), new Vector(0, -0.3), playerBody, (float) Math.PI, 0.5f, 1,
				Textures.PLAYER_LEG_1, Textures.PLAYER_LEG_1_COLMESH);
		legRight1.setRelativeZIndex(-1);

		Part legRight2 = new Part(new Vector(0, -0.25), new Vector(0, -0.2), legRight1, 0, 0.5f, 1, Textures.PLAYER_LEG_2,
				Textures.PLAYER_LEG_2_COLMESH);
		

		armLeft1.setRelativeZIndex(-1f);
		
		
		Part armLeft2 = new Part(new Vector(0, -0.2), new Vector(0, -0.2), armLeft1, 0, 0.4f, 1, Textures.PLAYER_ARM,
				Textures.PLAYER_ARM_COLMESH);
		Part armRight2 = new Part(new Vector(0, -0.2), new Vector(0, -0.2), armRight1, 0, 0.4f, 1, Textures.PLAYER_ARM,
				Textures.PLAYER_ARM_COLMESH);

		legLeft1.setRotLimit(Math.PI / 2, 5 * Math.PI / 4);
		legLeft2.setRotLimit(0, 3 * Math.PI / 4);
		legRight1.setRotLimit(Math.PI / 2, 5 * Math.PI / 4);
		legRight2.setRotLimit(0, 3 * Math.PI / 4);

		armRight1.setRotLimit(Math.PI / 4, 3 * Math.PI / 2);
		armRight2.setRotLimit(-3 * Math.PI / 4, 0);
		armLeft1.setRotLimit(Math.PI / 4, 3 * Math.PI / 2);
		armLeft2.setRotLimit(-3 * Math.PI / 4, 0);
		
		Part head = new Part(new Vector(0, -0.3), new Vector(0, -0.3), playerBody, 0, 0.5f, 1, Textures.PLAYER_HEAD,
				Textures.PLAYER_HEAD_COLMESH);

		head.setRotLimit(-Math.PI / 4, Math.PI / 4);

		armLeft1.connected.add(armLeft2);
		armRight1.connected.add(armRight2);

		legLeft1.connected.add(legLeft2);
		legRight1.connected.add(legRight2);

		playerBody.connected.add(armLeft1);
		playerBody.connected.add(armRight1);
		playerBody.connected.add(legLeft1);
		playerBody.connected.add(legRight1);
		playerBody.connected.add(head);

		player.part = playerBody;

		loadSnake();
		loadSpaceShip();

		objects.add(player);

		active = Optional.of(getPlayer().part);
	}

	public void loadSpaceShip() {
		GameObject spaceShip = new GameObject(this, new Vector(0, 0), "SpaceShip");

		Part mainRoom = new SpaceShipMainComputerRoom(spaceShip);
		spaceShip.part = mainRoom;

		objects.add(spaceShip);
	}

	public void loadSnake() {
		GameObject obj = new GameObject(this, new Vector(10, 0), "Snake");
		Part first = new Part(new Vector(0, 0), new Vector(0, 0), obj, 0, 0.7f, 10f,
				ImageLoader.getImage("/DebugSnakeThing/Part1.png"),
				ImageLoader.getImage("/DebugSnakeThing/ColMesh.png"));
		obj.part = first;

		/*
		 * Part second = new Part(new Vector(0, 0.7), new Vector(0, 0), first,
		 * 0, 0.7f, 10000f, ImageLoader.getImage("/DebugSnakeThing/Part2.png"),
		 * ImageLoader.getImage("/DebugSnakeThing/ColMesh.png")); //
		 * first.connected.add(second);
		 * 
		 * Part third = new Part(new Vector(0, 0.7), new Vector(0, 0), second,
		 * 0, 0.7f, 10000f, ImageLoader.getImage("/DebugSnakeThing/Part3.png"),
		 * ImageLoader.getImage("/DebugSnakeThing/ColMesh.png"));
		 * second.connected.add(third);
		 * 
		 * Part fourth = new Part(new Vector(0, 0.7), new Vector(0, 0), third,
		 * 0, 0.7f, 10000f, ImageLoader.getImage("/DebugSnakeThing/Part4.png"),
		 * ImageLoader.getImage("/DebugSnakeThing/ColMesh.png"));
		 * third.connected.add(fourth);
		 */

		objects.add(obj);
	}

	public void openGui(Gui gui) {
		this.gui = gui;
	}

	public void closeGui() {
		this.gui = null;
	}

	public Gui getGui() {
		return this.gui;
	}

	public boolean hasGui() {
		return gui != null;
	}

	public void draw(Graphics g_, int width, int height) {

		this.width = width;
		this.height = height;

		Graphics2D g = (Graphics2D) g_;

		g.setColor(Color.black);
		g.fillRect(0, 0, width, height);

		Composite c = g.getComposite();
		if (hasGui()) {
			g.setComposite(AlphaComposite.SrcOver.derive(0.3f));
		}
		BufferedImage gameRendered = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics grg = gameRendered.getGraphics();

		grg.drawImage(background, (int) -camPos.getX() - background.getWidth() / 2,
				(int) -camPos.getY() - background.getHeight() / 2, null);
		objects.forEach(obj -> obj.draw(grg, width, height));

		g.drawImage(gameRendered, 0, 0, null);

		if (hasGui()) {
			g.setComposite(c);

			getGui().draw((Graphics2D) g, width, height);

		}

		camPos = getPlayer().posInSpace;
	}

	public Part getPlayerLeftLastArm() {
		return getPlayer().part.connected.get(0).connected.get(0);
	}

	public void updateWorld(float timeStep) {
		tick++;
		objects.forEach(o -> o.update(timeStep));

	}

	public void updateCamera(float timeStep) {

		camVel = camVel.add(camAccel.mul(timeStep));
		camVel = camVel.div((float) Math.pow(304f, timeStep));
		camPos = camPos.add(camVel.mul(timeStep));

		if (movingCamToPlayer) {
			camVel = camVel.add(getPlayer().posInSpace.sub(camPos).mul(timeStep));
		}

	}

	public ArrayList<CollisionLineSegment> getCollisions(LineSegment line) {
		ArrayList<CollisionLineSegment> collisions = new ArrayList<CollisionLineSegment>();
		for (GameObject obj : objects)
			collisions.addAll(obj.getIntersectors(line));
		return collisions;
	}

	public Player getPlayer() {
		return (Player) objects.stream().filter(p -> p instanceof Player).findAny().get();
	}

	public Vector getCamPosCenterInSpace() {
		return (new Vector(width / 2 / zoom, height / 2 / zoom)).sub(camPos);
	}

	public Vector transformSpaceToScreen(Vector space) {
		return space.add(getCamPosCenterInSpace()).mul(zoom);
	}

	public Vector transformScreenToSpace(Vector screen) {
		return screen.div(zoom).sub(getCamPosCenterInSpace());
	}

}
