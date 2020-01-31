package me.modmuss50.voyager.mixin;

import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(StructureManager.class)
public abstract class StructureManagerMixin {

	/**
	 * Another option would be to make this an {@link java.util.concurrent.ConcurrentHashMap} however that does not for null values (used via the vanilla code)
	 * So {@link java.util.Collections#synchronizedMap} seemed like a good choice, however that is also synchronized on lookup, this may slow down performance
	 */
	@Shadow
	@Final
	private Map<Identifier, Structure> structures;

	// Just a marker object used to synchronize on
	private final Object structuresLock = new Object();

	@Overwrite
	public Structure getStructure(Identifier identifier) {
		/*
			The logic here works the same as vanilla,
			also inserts null into the map if it fails to load the structure (same as vanilla) to prevent trying to load an invalid structure again
		 */
		Structure structure = structures.get(identifier);
		if (structure == null) {
			structure = loadStructureFromFile(identifier);
			if (structure == null) {
				structure = loadStructureFromResource(identifier);
			}

			/*
			  The actual fix
			  Synchronize the insertion into the map as this can be called from more than one thread
			 */
			synchronized (structuresLock) {
				structures.put(identifier, structure);
			}
		}
		return structure;
	}

	@Shadow
	protected abstract Structure loadStructureFromFile(Identifier id);

	@Shadow
	protected abstract Structure loadStructureFromResource(Identifier id);
}
