package net.blancworks.figura.lua.api.world.block;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.LuaAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaTable;

//Not implemented yet
//Eventually provides blocks, with properties and all, to lua.
public class BlockAPI implements LuaAPI {
    private static final BlockAPI INSTANCE = new BlockAPI();
    public static BlockAPI getInstance() {
        return INSTANCE;
    }

    private static final ReadOnlyLuaTable globalLuaTable = new ReadOnlyLuaTable(new LuaTable() {{
        
        

    }});


    public Identifier getID() {
        return new Identifier("default", "blocks");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {
        return globalLuaTable;
    }
    
}
