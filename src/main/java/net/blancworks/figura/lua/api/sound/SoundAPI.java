package net.blancworks.figura.lua.api.sound;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.LuaAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class SoundAPI implements LuaAPI {
    private static final SoundAPI INSTANCE = new SoundAPI();
    public static SoundAPI getInstance() {
        return INSTANCE;
    }

    public static HashMap<String, SoundEvent> soundEvents = new HashMap<String, SoundEvent>() {{
        for (Identifier id : Registry.SOUND_EVENT.getIds()) {
            SoundEvent type = Registry.SOUND_EVENT.get(id);
            put(id.getPath(), type);
            put(id.toString(), type);
        }
    }};


    public Identifier getID() {
        return new Identifier("default", "sound");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("playSound", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    
                    if(script.soundSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_SOUND_EFFECTS_ID))
                        return NIL;
                    script.soundSpawnCount++;
                    
                    SoundEvent targetEvent = soundEvents.get(arg1.checkjstring());
                    if (targetEvent == null)
                        return NIL;

                    FloatArrayList floats = LuaUtils.getFloatsFromTable(arg2.checktable());

                    if (floats.size() != 5)
                        return NIL;

                    World w = MinecraftClient.getInstance().world;

                    w.playSound(
                            floats.getFloat(0), floats.getFloat(1), floats.getFloat(2),
                            targetEvent, SoundCategory.PLAYERS,
                            floats.getFloat(3), floats.getFloat(4), true
                    );

                    return NIL;
                }
            });
        }});
    }

}
