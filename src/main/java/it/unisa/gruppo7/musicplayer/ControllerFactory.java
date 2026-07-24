package it.unisa.gruppo7.musicplayer;

import java.lang.reflect.Constructor;

import it.unisa.gruppo7.musicplayer.musicplayerfacade.MusicPlayerFacade;
import javafx.util.Callback;

/**
 * JavaFX controller factory performing constructor-based dependency injection.
 * When a controller declares a public constructor accepting a {@link MusicPlayerFacade},
 * the single shared facade instance is injected into it; otherwise the controller's
 * no-argument constructor is used. This lets the application wire one facade instance
 * into the whole FXML controller graph without any static/global access point,
 * replacing the former Singleton with proper dependency injection.
 *
 */
public class ControllerFactory implements Callback<Class<?>, Object> {

    /** The shared facade instance injected into facade-aware controllers. */
    private final MusicPlayerFacade facade;

    /**
     * Creates a factory that injects the given facade instance.
     *
     * @param facade the shared facade to inject into the controllers.
     */
    public ControllerFactory(MusicPlayerFacade facade) {
        this.facade = facade;
    }

    /**
     * Instantiates the requested controller, injecting the facade when the controller
     * declares a matching constructor, or falling back to its no-argument constructor.
     *
     * @param type the controller class requested by the {@code FXMLLoader}.
     * @return a new controller instance.
     */
    @Override
    public Object call(Class<?> type) {
        try {
            try {
                Constructor<?> facadeCtor = type.getConstructor(MusicPlayerFacade.class);
                return facadeCtor.newInstance(facade);
            } catch (NoSuchMethodException noFacadeConstructor) {
                return type.getDeclaredConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to instantiate controller: " + type.getName(), e);
        }
    }
}
