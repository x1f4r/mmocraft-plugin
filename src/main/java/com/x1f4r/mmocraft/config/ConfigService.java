package com.x1f4r.mmocraft.config;

import java.util.List;

public interface ConfigService {
    void loadConfig();
    String getString(String path);
    int getInt(String path);
    double getDouble(String path);
    boolean getBoolean(String path);
    List<String> getStringList(String path);
    void reloadConfig();
}
