import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import java.io.File;

public class TestNbt {
    public static void main(String[] args) throws Exception {
        for (String name : new String[]{"tavern.nbt", "warrior_house_1.nbt", "warrior_house_2.nbt"}) {
            File f = new File("src/main/resources/data/epicvanguard/structures/" + name);
            if (!f.exists()) {
                System.out.println(name + " NOT FOUND!");
                continue;
            }
            CompoundTag tag = NbtIo.readCompressed(f);
            System.out.println("=== " + name + " ===");
            System.out.println("Size: " + tag.get("size"));
            if (tag.contains("blocks")) {
                System.out.println("Blocks count: " + tag.getList("blocks", 10).size());
            }
            if (tag.contains("palette")) {
                System.out.println("Palette size: " + tag.getList("palette", 10).size());
                for (int i = 0; i < tag.getList("palette", 10).size(); i++) {
                    System.out.println("  " + tag.getList("palette", 10).getCompound(i).getString("Name"));
                }
            }
        }
    }
}
