package io.github.mfedenko09.roastlands.cli;

import io.github.mfedenko09.roastlands.core.content.ContentPack;
import io.github.mfedenko09.roastlands.core.topic.TopicGraph;

record GameContent(TopicGraph topics, ContentPack pack) {
}
