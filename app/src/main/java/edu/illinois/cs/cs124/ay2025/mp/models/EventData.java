package edu.illinois.cs.cs124.ay2025.mp.models;

import androidx.annotation.NonNull;

public record EventData(
    @NonNull String id,
    @NonNull String seriesId,
    @NonNull String title,
    @NonNull String start,
    @NonNull String location,
    @NonNull String description,
    @NonNull String[] categories,
    @NonNull String source,
    @NonNull String url,
    boolean virtual) {}
