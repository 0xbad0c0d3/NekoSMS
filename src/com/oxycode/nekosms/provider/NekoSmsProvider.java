package com.oxycode.nekosms.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class NekoSmsProvider extends ContentProvider {
    private static final int FILTERS_ITEM_ID = 0;
    private static final int FILTERS_ITEMS_ID = 1;
    private static final int BLOCKED_ITEM_ID = 2;
    private static final int BLOCKED_ITEMS_ID = 3;

    private static final UriMatcher sUriMatcher;
    private NekoSmsDbHelper mDatabaseHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(NekoSmsContract.AUTHORITY, NekoSmsContract.Filters.TABLE, FILTERS_ITEMS_ID);
        sUriMatcher.addURI(NekoSmsContract.AUTHORITY, NekoSmsContract.Filters.TABLE + "/#", FILTERS_ITEM_ID);
        sUriMatcher.addURI(NekoSmsContract.AUTHORITY, NekoSmsContract.Blocked.TABLE, BLOCKED_ITEMS_ID);
        sUriMatcher.addURI(NekoSmsContract.AUTHORITY, NekoSmsContract.Blocked.TABLE + "/#", BLOCKED_ITEM_ID);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new NekoSmsDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
        case FILTERS_ITEM_ID:
            queryBuilder.appendWhere(NekoSmsContract.Filters._ID + "=" + uri.getLastPathSegment());
        case FILTERS_ITEMS_ID:
            queryBuilder.setTables(NekoSmsContract.Filters.TABLE);
            break;
        case BLOCKED_ITEM_ID:
            queryBuilder.appendWhere(NekoSmsContract.Blocked._ID + "=" + uri.getLastPathSegment());
        case BLOCKED_ITEMS_ID:
            queryBuilder.setTables(NekoSmsContract.Blocked.TABLE);
            break;
        default:
            throw new IllegalArgumentException("Invalid query URI: " + uri);
        }

        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        long row;
        switch (sUriMatcher.match(uri)) {
        case FILTERS_ITEMS_ID:
            row = db.insert(NekoSmsContract.Filters.TABLE, null, values);
            break;
        case BLOCKED_ITEMS_ID:
            row = db.insert(NekoSmsContract.Blocked.TABLE, null, values);
            break;
        default:
            throw new IllegalArgumentException("Invalid insert URI: " + uri);
        }

        Uri newUri = ContentUris.withAppendedId(uri, row);
        if (row >= 0) {
            getContext().getContentResolver().notifyChange(newUri, null);
        }
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int deletedRows;
        switch (sUriMatcher.match(uri)) {
        case FILTERS_ITEM_ID:
            selection = getCombinedSelectionString(NekoSmsContract.Filters._ID, uri, selection);
        case FILTERS_ITEMS_ID:
            deletedRows = db.delete(NekoSmsContract.Filters.TABLE, selection, selectionArgs);
            break;
        case BLOCKED_ITEM_ID:
            selection = getCombinedSelectionString(NekoSmsContract.Blocked._ID, uri, selection);
        case BLOCKED_ITEMS_ID:
            deletedRows = db.delete(NekoSmsContract.Blocked.TABLE, selection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Invalid delete URI: " + uri);
        }

        if (selection == null || deletedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deletedRows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int updatedRows;
        switch (sUriMatcher.match(uri)) {
        case FILTERS_ITEM_ID:
            selection = getCombinedSelectionString(NekoSmsContract.Filters._ID, uri, selection);
        case FILTERS_ITEMS_ID:
            updatedRows = db.update(NekoSmsContract.Filters.TABLE, values, selection, selectionArgs);
            break;
        case BLOCKED_ITEM_ID:
            selection = getCombinedSelectionString(NekoSmsContract.Blocked._ID, uri, selection);
        case BLOCKED_ITEMS_ID:
            updatedRows = db.update(NekoSmsContract.Blocked.TABLE, values, selection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Invalid update URI: " + uri);
        }

        if (updatedRows > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updatedRows;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    private String getCombinedSelectionString(String idColumnName, Uri uri, String selection) {
        String profileWhere = idColumnName + "=" + uri.getLastPathSegment();
        if (TextUtils.isEmpty(selection)) {
            return profileWhere;
        } else {
            return profileWhere + " AND " + selection;
        }
    }
}
