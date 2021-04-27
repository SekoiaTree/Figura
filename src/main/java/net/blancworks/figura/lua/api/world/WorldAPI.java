package net.blancworks.figura.lua.api.world;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.LuaUtils;
import net.blancworks.figura.lua.api.LuaAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.world.block.BlockStateAPI;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class WorldAPI implements LuaAPI {
    private static final WorldAPI INSTANCE = new WorldAPI();
    public static WorldAPI getInstance() {
        return INSTANCE;
    }
    
    private static World getWorld(){
        return MinecraftClient.getInstance().world;
    }

    private static ReadOnlyLuaTable globalLuaTable;
    
    private static World lastWorld;


    public Identifier getID() {
        return new Identifier("default", "world");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {
        
        if(getWorld() != lastWorld)
            updateGlobalTable();
        
        return globalLuaTable;
    }
    
    public static void updateGlobalTable(){
        globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
            set("getBlockState", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaTable getPosTable = arg.checktable();
                    BlockPos pos = LuaUtils.getBlockPosFromTable(getPosTable);

                    if(pos == null) return NIL;

                    World w = getWorld();

                    if(!w.isChunkLoaded(pos)) return NIL;

                    BlockState state = w.getBlockState(pos);

                    return BlockStateAPI.getTable(state);
                }
            });

            set("getRedstonePower", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaTable getPosTable = arg.checktable();
                    BlockPos pos = LuaUtils.getBlockPosFromTable(getPosTable);

                    if(pos == null) return NIL;

                    World w = getWorld();

                    if(!w.isChunkLoaded(pos)) return NIL;

                    return LuaNumber.valueOf(w.getReceivedRedstonePower(pos));
                }
            });

            set("getStrongRedstonePower", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    LuaTable getPosTable = arg.checktable();
                    BlockPos pos = LuaUtils.getBlockPosFromTable(getPosTable);

                    if(pos == null) return NIL;

                    World w = getWorld();

                    if(!w.isChunkLoaded(pos)) return NIL;

                    return LuaNumber.valueOf(w.getReceivedStrongRedstonePower(pos));
                }
            });


            set("getTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getTime());
                }
            });


            set("getTimeOfDay", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getTimeOfDay());
                }
            });

            set("getMoonPhase", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getMoonPhase());
                }
            });

            //Looks like this just... Gets the time??? Confused on this one.
            set("getLunarTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaNumber.valueOf(getWorld().getLunarTime());
                }
            });

            set("getRainGradient", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    return LuaNumber.valueOf(getWorld().getRainGradient((float)(a.checkdouble())));
                }
            });

            set("isLightning", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    return LuaBoolean.valueOf(getWorld().isThundering());
                }
            });

            set("getLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    BlockPos pos = LuaUtils.getBlockPosFromTable(a.checktable());

                    if(pos == null)
                        return LuaInteger.valueOf(0);
                    if(!getWorld().isChunkLoaded(pos)) return NIL;


                    getWorld().calculateAmbientDarkness();
                    int dark = getWorld().getAmbientDarkness();
                    int realLight = getWorld().getLightingProvider().getLight(pos, dark);

                    return LuaInteger.valueOf(realLight);
                }
            });

            set("getSkyLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    BlockPos pos = LuaUtils.getBlockPosFromTable(a.checktable());

                    if(pos == null)
                        return LuaInteger.valueOf(0);
                    if(!getWorld().isChunkLoaded(pos)) return NIL;

                    return LuaInteger.valueOf(getWorld().getLightLevel(LightType.SKY, pos));
                }
            });

            set("getBlockLightLevel", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {
                    BlockPos pos = LuaUtils.getBlockPosFromTable(a.checktable());

                    if(pos == null)
                        return LuaInteger.valueOf(0);
                    if(!getWorld().isChunkLoaded(pos)) return NIL;

                    return LuaInteger.valueOf(getWorld().getLightLevel(LightType.BLOCK, pos));
                }
            });

            set("getBiomeID", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue a) {

                    BlockPos pos = LuaUtils.getBlockPosFromTable(a.checktable());

                    if(pos == null)
                        return NIL;
                    if(!getWorld().isChunkLoaded(pos)) return NIL;

                    Biome b = getWorld().getBiome(pos);

                    if(b == null)
                        return NIL;

                    Identifier id = getWorld().getRegistryManager().get(Registry.BIOME_KEY).getId(b);

                    return LuaString.valueOf(id.toString());
                }
            });

        }});
    }
    
}
