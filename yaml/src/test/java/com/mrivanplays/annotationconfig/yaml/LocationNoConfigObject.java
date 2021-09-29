package com.mrivanplays.annotationconfig.yaml;

import com.mrivanplays.annotationconfig.core.serialization.DeserializeConstructor;

public class LocationNoConfigObject {

  private String world;
  private int x, y, z;

  @DeserializeConstructor
  public LocationNoConfigObject(String world, int x, int y, int z) {
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public String getWorld() {
    return world;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public int getZ() {
    return z;
  }
}
