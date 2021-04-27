package net.blancworks.figura.lua.api;

import net.blancworks.figura.lua.CustomScript;
import net.minecraft.util.Identifier;

public interface LuaAPI {
    Identifier getID();

    ReadOnlyLuaTable getForScript(CustomScript script);
}
