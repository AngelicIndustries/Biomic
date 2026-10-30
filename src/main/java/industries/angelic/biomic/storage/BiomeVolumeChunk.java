package industries.angelic.biomic.storage;

/** Internal attachment installed on vanilla chunks for ChunkAPI serialization. */
public interface BiomeVolumeChunk {

    BiomeVolume biomic$getBiomeVolume();

    void biomic$setBiomeVolume(BiomeVolume volume);
}
