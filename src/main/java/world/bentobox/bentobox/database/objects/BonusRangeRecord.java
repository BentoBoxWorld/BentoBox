package world.bentobox.bentobox.database.objects;

import com.google.gson.annotations.JsonAdapter;

import marcono1234.gson.recordadapter.RecordTypeAdapterFactory;

/**
 * Record for bonus ranges
 * @author tastybento
 * @param id an id to identify this bonus
 * @param range the additional bonus range
 * @param message the reference key to a locale message related to this bonus. May be blank.
 */
@JsonAdapter(RecordTypeAdapterFactory.class)
public record BonusRangeRecord(String uniqueId, int range, String message) {}
