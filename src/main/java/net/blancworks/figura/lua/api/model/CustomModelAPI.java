package net.blancworks.figura.lua.api.model;

import net.blancworks.figura.lua.CustomScript;
import net.blancworks.figura.lua.api.LuaAPI;
import net.blancworks.figura.lua.api.ReadOnlyLuaTable;
import net.blancworks.figura.lua.api.ScriptLocalAPITable;
import net.blancworks.figura.lua.api.VectorAPI;
import net.blancworks.figura.models.CustomModelPart;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class CustomModelAPI implements LuaAPI {
    private static final CustomModelAPI INSTANCE = new CustomModelAPI();
    public static CustomModelAPI getInstance() {
        return INSTANCE;
    }

    public Identifier getID() {
        return new Identifier("default", "model");
    }

    public ReadOnlyLuaTable getForScript(CustomScript script) {
        ScriptLocalAPITable producedTable = new ScriptLocalAPITable(script, new LuaTable() {{
            for (CustomModelPart part : script.playerData.model.allParts) {
                set(part.name, new CustomModelPartTable(part));
            }
        }});

        return producedTable;
    }

    public static ReadOnlyLuaTable getTableForCustomPart(CustomModelPart part) {
        CustomModelPartTable producedTable = new CustomModelPartTable(part);
        return producedTable;
    }

    private static class CustomModelPartTable extends ReadOnlyLuaTable {
        CustomModelPart targetPart;

        public CustomModelPartTable(CustomModelPart part) {
            super();
            targetPart = part;
            super.setTable(getTable());
        }

        public LuaTable getTable() {
            LuaTable ret = new LuaTable();
            
            int index = 1;
            for (CustomModelPart child : targetPart.children) {
                CustomModelPartTable tbl = new CustomModelPartTable(child);
                ret.set(child.name, tbl);
                ret.set(index++, tbl);
            }
            
            ret.set("getPos", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.getVector(targetPart.pos);
                }
            });

            ret.set("setPos", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    Vector3f newPos = VectorAPI.checkVec3(arg1);
                    targetPart.pos = newPos;
                    return NIL;
                }
            });

            ret.set("getPivot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.getVector(targetPart.pivot);
                }
            });

            ret.set("setPivot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    Vector3f newPivot = VectorAPI.checkVec3(arg1);
                    targetPart.pivot = newPivot;
                    return NIL;
                }
            });

            ret.set("getColor", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.getVector(targetPart.color);
                }
            });

            ret.set("setColor", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    Vector3f newColor = VectorAPI.checkVec3(arg1);
                    targetPart.color = newColor;
                    return NIL;
                }
            });

            ret.set("getScale", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.getVector(targetPart.scale);
                }
            });

            ret.set("setScale", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    Vector3f newScale = VectorAPI.checkVec3(arg1);
                    targetPart.scale = newScale;
                    return NIL;
                }
            });

            ret.set("getRot", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.getVector(targetPart.rot);
                }
            });

            ret.set("setRot", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    Vector3f newRot = VectorAPI.checkVec3(arg1);
                    targetPart.rot = newRot;
                    return NIL;
                }
            });

            ret.set("getUV", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return VectorAPI.getVector(targetPart.rot);
                }
            });

            ret.set("setUV", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    Vec2f newUV = VectorAPI.checkVec2f(arg1);
                    targetPart.uOffset = newUV.x;
                    targetPart.vOffset = newUV.y;
                    return NIL;
                }
            });

            ret.set("getParentType", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaString.valueOf(targetPart.parentType.toString());
                }
            });

            ret.set("setParentType", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.parentType = CustomModelPart.ParentType.valueOf(arg1.checkjstring());
                    return NIL;
                }
            });

            ret.set("getMimicMode", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.isMimicMode);
                }
            });

            ret.set("setMimicMode", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1) {
                    targetPart.isMimicMode = arg1.checkboolean();
                    return NIL;
                }
            });

            ret.set("getEnabled", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    return LuaBoolean.valueOf(targetPart.visible);
                }
            });

            ret.set("setEnabled", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    targetPart.visible = arg.checkboolean();
                    return null;
                }
            });
            
            return ret;
        }
    }
}
