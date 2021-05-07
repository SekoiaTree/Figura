package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class RendererAPI {

    public static Identifier getID() {
        return new Identifier("default", "renderer");
    }

    public static ReadOnlyLuaTable getForScript(CustomScript script) {
        return new ReadOnlyLuaTable(new LuaTable(){{
            set("setShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if(arg == NIL)
                        script.customShadowSize = null;
                    script.customShadowSize = arg.checknumber().tofloat();
                    return NIL;
                }
            });
            
            set("getShadowSize", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    if(script.customShadowSize == null)
                        return NIL;
                    return LuaNumber.valueOf(script.customShadowSize);
                }
            });
            set("isThirdPerson", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaValue.valueOf(MinecraftClient.getInstance().gameRenderer.getCamera().isThirdPerson());
                }
            });
            set("isExterior", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    // IntelliJ complains that getUuid() can cause a NPE, but through a bit of testing it probably doesn't happen
                    return LuaValue.valueOf(MinecraftClient.getInstance().player.getUuid() != script.playerData.playerId || MinecraftClient.getInstance().gameRenderer.getCamera().isThirdPerson());
                }
            });
        }});
    }
}
