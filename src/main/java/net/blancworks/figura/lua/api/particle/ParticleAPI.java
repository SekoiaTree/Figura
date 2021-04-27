package net.blancworks.figura.lua.api.particle;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.LuaAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.trust.PlayerTrustManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class ParticleAPI implements LuaAPI {
    private static final ParticleAPI INSTANCE = new ParticleAPI();
    public static ParticleAPI getInstance() {
        return INSTANCE;
    }

    public static HashMap<String, DefaultParticleType> particleTypes = new HashMap<String, DefaultParticleType>(){{
        for (Identifier id : Registry.PARTICLE_TYPE.getIds()) {
            ParticleType<?> type = Registry.PARTICLE_TYPE.get(id);
            if(type instanceof DefaultParticleType) {
                put(id.getPath(), (DefaultParticleType) type);
                put(id.toString(), (DefaultParticleType) type);
            }
        }
    }};
    
    public Identifier getID() {
        return new Identifier("default", "particle");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable() {{
            set("addParticle", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    if(!arg1.isstring() || !arg2.istable())
                        return NIL;

                    if(script.particleSpawnCount > script.playerData.getTrustContainer().getIntSetting(PlayerTrustManager.MAX_PARTICLES_ID))
                        return NIL;
                    script.particleSpawnCount++;
                    

                    DefaultParticleType targetType = particleTypes.get(arg1.checkjstring());
                    if(targetType == null)
                        return NIL;

                    FloatArrayList floats = LuaUtils.getFloatsFromTable(arg2.checktable());

                    if(floats.size() != 6)
                        return NIL;

                    World w = MinecraftClient.getInstance().world;

                    w.addParticle(targetType,
                            floats.getFloat(0),floats.getFloat(1),floats.getFloat(2),
                            floats.getFloat(3),floats.getFloat(4),floats.getFloat(5)
                    );

                    return NIL;
                }
            });
        }});
    }
}
