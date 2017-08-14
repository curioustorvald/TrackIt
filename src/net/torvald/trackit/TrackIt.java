package net.torvald.trackit;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import net.torvald.terrarum.langpack.Lang;

/**
 * Reference: http://www.mindreadings.com/ControlDemo/BasicTrack.html
 *
 *
 * This program will run optimally on decent machine, which can maintain
 * constant 60 frames per second.
 *
 * Created by minjaesong on 2017-08-03.
 */
public class TrackIt extends Game {

    public static LwjglApplicationConfiguration appConfig;

    public static final String sysLang = System.getProperty("user.language") + System.getProperty("user.country");

    public static void main(String[] args) {
        appConfig = new LwjglApplicationConfiguration();

        appConfig.width = 960;
        appConfig.height = 540;
        appConfig.foregroundFPS = 60;
        appConfig.backgroundFPS = 60;
        appConfig.resizable = false;
        appConfig.title = "Track It! — " + Lang.INSTANCE.get("MENU_IO_LOADING");

        new LwjglApplication(new TrackIt(), appConfig);
    }


    @Override
    public void create() {
        Gdx.graphics.setTitle("Track It! — " + Lang.INSTANCE.get("MENU_IO_LOADING"));

        setScreen(TaskMain.INSTANCE);
    }
}
