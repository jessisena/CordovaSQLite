package net.orworks.cordovaplugins.cordovasqlite;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;
import android.database.Cursor;
import android.database.sqlite.*;
import android.util.Base64;

/**
 * This class handles connection with a SQLite database on the device. The database can be residing on the internal or external storage.
 *
 * @author Samik
 */
public class CordovaSQLite extends CordovaPlugin
{
    CallbackContext _callbackContext = null;
    SQLiteDatabase myDb = null; // Database object
    private static final String TAG = "CordovaSQLite";
    private static final boolean I = true;

    /**
     * Executes the request and returns PluginResult.
     * Notes: overriding a different execute() method to avoid blocking main thread. This is suggested
     * in: https://issues.apache.org/jira/browse/CB-7109
     *
     * @param action          The action to execute.
     * @param rawArgs         Raw JSON arguments for the plugin as String.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return Returns true to indicate successful axecution (which might have resulted in error),
     * false results in a "MethodNotFound" error.
     */
    public boolean execute (String action, final String rawArgs, CallbackContext callbackContext)
    {
        if(I) Log.d(TAG, "Plugin called for: " + action);

        _callbackContext = callbackContext;

        if (action.equals("openDatabase"))
        {
            cordova.getThreadPool().execute(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(rawArgs);
                            String fullDBPath = jsonArray.getString(0);
                            boolean toCreate = (jsonArray.getInt(1) != 0);
                            openDatabase(fullDBPath, toCreate);
                        }
                        catch (JSONException ex){}
                    }
                }
            );
            return true;
        }
        else if (action.equals("execQuerySingleResult"))
        {
            cordova.getThreadPool().execute(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(rawArgs);
                            String query = jsonArray.getString(0);
                            String[] argList = getStringArray(jsonArray.getJSONArray(1));
                            execQuerySingleResult(query, argList);
                        }
                        catch (JSONException ex){}
                    }
                }
            );
            return true;
        }
        else if (action.equals("execQueryArrayResult"))
        {
            cordova.getThreadPool().execute(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(rawArgs);
                            String query = jsonArray.getString(0);
                            String[] argList = getStringArray(jsonArray.getJSONArray(1));
                            execQueryArrayResult(query, argList);
                        }
                        catch (JSONException ex){}
                    }
                }
            );
            return true;
        }
        else if (action.equals("execQueryNoResult"))
        {
            cordova.getThreadPool().execute(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            JSONArray jsonArray = new JSONArray(rawArgs);
                            String[] queries = getStringArray(jsonArray);
                            execQueryNoResult(queries);
                        }
                        catch (JSONException ex){}
                    }
                }
            );
            return true;
        }
        else if (action.equals("closeDB"))
        {
            this.closeDB();
            return true;
        }

        return false;
    }

    /**
     * Open a database.
     *
     * @param fullDBFilePath
     */
    private void openDatabase (String fullDBFilePath, boolean toCreate)
    {
        // If database is open, then close it
        if (this.myDb != null)
        {
            try
            {
                this.myDb.close();
            }
            catch (SQLiteException ex)
            {
                // Just catch and ignore the exception.
                Log.d(TAG, ex.getMessage());
                this.myDb = null;
            }
        }

        // Check if we have got a file URL (i.e., a string starting with file://).
        // In that case, we will discard the file:// part.
        if (fullDBFilePath.startsWith("file://"))
            fullDBFilePath = fullDBFilePath.substring(7);
        if(I) Log.d(TAG, "Opening database: " + fullDBFilePath);

        try
        {
            if (toCreate)
                myDb = SQLiteDatabase.openDatabase(fullDBFilePath, null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            else
                myDb = SQLiteDatabase.openDatabase(fullDBFilePath, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            _callbackContext.success();
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, "Can't open database: " + ex.getMessage());
            _callbackContext.error(ex.getMessage());
        }
    }

    /**
     * Exec query to get a single result String value.
     *
     * @param query
     * @param args
     * @return result.
     */
    private void execQuerySingleResultString (String query, String[] args)
    {
        if(I) Log.d(TAG, "Executing query: " + query + " with arg: " + args[0]);
        try
        {
            String result = null;
            Cursor cursor = myDb.rawQuery(query, args);
            if (cursor.moveToFirst())
                result = cursor.getString(0);
            cursor.close();
            _callbackContext.success(result);
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, ex.getMessage());
            _callbackContext.error(ex.getMessage());
        }
    }

    /**
     * Exec query to get a single result value (String or BLOB)
     *
     * @param query
     * @param args
     * @return result.
     */
    private void execQuerySingleResult (String query, String[] args)
    {
        if(I) Log.d(TAG, "Executing query: " + query + " with arg: " + args[0]);
        try
        {
            String result = null;
            Cursor cursor = myDb.rawQuery(query, args);
            if (cursor.moveToFirst()){
                int type = cursor.getType(0);

                if(type == cursor.FIELD_TYPE_BLOB){
                    byte[] resultBLOB = cursor.getBlob(0);
                    result = Base64.encodeToString(resultBLOB, Base64.DEFAULT);
                    if(I) Log.d(TAG, "result trnasformat BLOB: "+result);

                    String text = new String(resultBLOB, "UTF-8");
                    if(I) Log.d(TAG, "result text BLOB: "+text);

                    cursor.close();
                    PluginResult resultPlugin = new PluginResult(PluginResult.Status.OK,
                        result);
                    //_callbackContext.success(result);
                    resultPlugin.setKeepCallback(true);
                    _callbackContext.sendPluginResult(resultPlugin);
                    resultPlugin.setKeepCallback(false);

                }else if(type == cursor. FIELD_TYPE_STRING){
                    result = cursor.getString(0);
                    cursor.close();
                    _callbackContext.success(result);

                }else{
                    if(I) Log.d(TAG, "Retorn tipus no contemplat (BLOB, String)");
                    cursor.close();
                    _callbackContext.error("Retorn tipus no contemplat (BLOB, String)");
                }

            }else{
                cursor.close();
                _callbackContext.success(result);
            }

        }catch (SQLiteException ex){
            Log.e(TAG, ex.getMessage());
            _callbackContext.error(ex.getMessage());
        }
    }

    /**
     * Execute a query and return a 2D JSON array. Rows are records and columns are data cols.
     *
     * @param query
     * @param args
     * @return
     */
    private void execQueryArrayResult (String query, String[] args)
    {

    	if(I) Log.d(TAG, "Executing query: " + query + " with arg: ");
    	/*for (String string : args)
    		if(I) Log.d(TAG, string);
    	*/

        try
        {
            Cursor cursor = myDb.rawQuery(query, args);

            String resultStr = "[";
            // If query result has rows
            if (cursor.moveToFirst())
            {
                int colCount = cursor.getColumnCount();
                do
                {
                    String val = cursor.getString(0);
                    String rowStr = (val == null ? "[null" : "[\"" + cursor.getString(0) + "\"");
                    for (int i = 1; i < colCount; i++)
                    {
                        val = cursor.getString(i);
                        rowStr += (val == null ? ", null" : ", \"" + cursor.getString(i) + "\"");
                    }
                    rowStr += "]";
                    resultStr += rowStr + ", ";
                    // Keep adding rows till we have around 7000 characters. Beyond that, we
                    // get a 'Syntax Error' when the result is passed to javascript.
                    // Can possibly go beyond 7000, haven't tried. Gives error at around 12000.
                    if (resultStr.length() > 7000)
                        break;
                } while (cursor.moveToNext());

                resultStr = resultStr.substring(0, resultStr.lastIndexOf(","));
            }
            resultStr += "]";
            if(I) Log.d(TAG, "Result rowcount=" + cursor.getCount());
            if(I) Log.d(TAG, "Result=" + resultStr);
            cursor.close();
            // Set up the result object.
            _callbackContext.success(resultStr);
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, ex.getMessage());
            _callbackContext.error(ex.getMessage());
        }
    }

    /**
     * Execute set of queries which return no value (like insert, update etc.)
     *
     * @param queries A string array containing the queries.
     */
    private void execQueryNoResult (String[] queries)
    {
        try
        {
            for (String query : queries)
            {
                if(I) Log.d(TAG, "Executing query: " + query);
                myDb.execSQL(query);
            }
            _callbackContext.success();
        }
        catch (SQLiteException ex)
        {
            Log.e(TAG, ex.getMessage());
            _callbackContext.error(ex.getMessage());
        }
    }

    /**
     * Closes a DB safely.
     *
     * @return
     */
    private void closeDB ()
    {
        if (this.myDb != null)
        {
            this.myDb.close();
            this.myDb = null;
        }
        _callbackContext.success();
    }

    /**
     * Convert a JSONArray object to a string array.
     *
     * @param array
     * @return
     * @throws JSONException
     */
    private String[] getStringArray (JSONArray array) throws JSONException
    {
        String[] strArray = new String[array.length()];
        for (int i = 0; i < strArray.length; i++)
            strArray[i] = array.getString(i);
        return strArray;
    }

    /**
     * Clean up and close database.
     */
    @Override
    public void onDestroy ()
    {
        if (this.myDb != null)
        {
            this.myDb.close();
            this.myDb = null;
        }
    }
}
