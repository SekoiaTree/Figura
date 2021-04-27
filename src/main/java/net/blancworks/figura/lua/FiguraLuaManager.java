package net.blancworks.figura.lua;

import net.blancworks.figura.PlayerData;
import net.blancworks.figura.lua.api.LuaEvent;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.VectorAPI;
import net.blancworks.figura.lua.api.model.*;
import net.blancworks.figura.lua.api.particle.ParticleAPI;
import net.blancworks.figura.lua.api.sound.SoundAPI;
import net.blancworks.figura.lua.api.world.WorldAPI;
import net.blancworks.figura.lua.api.world.entity.PlayerEntityAPI;
import net.minecraft.util.Identifier;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FiguraLuaManager {

    public static HashMap<Identifier, Function<CustomScript, ? extends ReadOnlyLuaTable>> apiSuppliers = new HashMap<Identifier, Function<CustomScript, ? extends ReadOnlyLuaTable>>();
    public static Map<String, Function<String, LuaEvent>> registeredEvents = new HashMap<String, Function<String, LuaEvent>>();

    //The globals for the entire lua system.
    public static Globals modGlobals;

    public static void initialize() {
        modGlobals = new Globals();
        modGlobals.load(new JseBaseLib());
        modGlobals.load(new PackageLib());
        modGlobals.load(new StringLib());
        modGlobals.load(new JseMathLib());

        LoadState.install(modGlobals);
        LuaC.install(modGlobals);

        LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);

        registerEvents();
        registerAPI();
    }

    public static void registerAPI() {
        apiSuppliers.put(ParticleAPI.getInstance().getID(), ParticleAPI.getInstance()::getForScript);
        apiSuppliers.put(CustomModelAPI.getInstance().getID(), CustomModelAPI.getInstance()::getForScript);
        apiSuppliers.put(VanillaModelAPI.getInstance().getID(), VanillaModelAPI.getInstance()::getForScript);
        apiSuppliers.put(PlayerEntityAPI.getInstance().getID(), PlayerEntityAPI.getInstance()::getForScript);
        apiSuppliers.put(WorldAPI.getInstance().getID(), WorldAPI.getInstance()::getForScript);
        apiSuppliers.put(ArmorModelAPI.getInstance().getID(), ArmorModelAPI.getInstance()::getForScript);
        apiSuppliers.put(ElytraModelAPI.getInstance().getID(), ElytraModelAPI.getInstance()::getForScript);
        apiSuppliers.put(ItemModelAPI.getInstance().getID(), ItemModelAPI.getInstance()::getForScript);
        apiSuppliers.put(VectorAPI.getInstance().getID(), VectorAPI.getInstance()::getForScript);
        apiSuppliers.put(SoundAPI.getInstance().getID(), SoundAPI.getInstance()::getForScript);
    }

    public static void registerEvents(){
        registerEvent("tick");
        registerEvent("render");

        registerEvent("onDamage");
    }

    public static void loadScript(PlayerData data, String content) {
        CustomScript newScript = new CustomScript(data, content);
        data.script = newScript;
    }

    public static void setupScriptAPI(CustomScript script) {
        for (Map.Entry<Identifier, Function<CustomScript, ? extends ReadOnlyLuaTable>> entry : apiSuppliers.entrySet()) {
            try {
                script.scriptGlobals.set(entry.getKey().getPath(), entry.getValue().apply(script));
            } catch (Exception e) {
                System.out.println("Failed to initialize script global " + entry.getKey().toString());
                e.printStackTrace();
            }
        }
    }

    public static void registerEvent(String name) {
        registeredEvents.put(name, LuaEvent::new);
    }

}
