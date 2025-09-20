package com.x1f4r.mmocraft.content;

import java.nio.file.Path;
import java.util.List;

public interface ContentPackService {

    ContentIndex reloadPacks();

    ContentIndex getContentIndex();

    List<ContentPack> getLoadedPacks();

    List<ContentPackIssue> getIssues();

    Path getContentRoot();
}
