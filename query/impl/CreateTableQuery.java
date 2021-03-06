package chakri.query.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import chakri.configuration.CatalogDatabaseHelper;
import chakri.model.DatabaseConstants;
import chakri.prompt.Column;
import chakri.prompt.IOManager;
import chakri.prompt.InternalColumn;
import chakri.prompt.InternalException;
import chakri.prompt.Result;
import chakri.prompt.Utils;
import chakri.query.interfaces.IQuery;

/**
 * Created by Chakriramoj on Apr 21, 2019
 *
 */



public class CreateTableQuery implements IQuery {
    public String tableName;
    public ArrayList<Column> columns;
    private boolean hasPrimaryKey;
    public String databaseName;

    public CreateTableQuery(String databaseName, String tableName, ArrayList<Column> columns, boolean hasPrimaryKey){
        this.tableName = tableName;
        this.columns = columns;
        this.hasPrimaryKey = hasPrimaryKey;
        this.databaseName = databaseName;
    }

    @Override
    public Result ExecuteQuery() {
        return new Result(1);
    }

    @Override
    public boolean ValidateQuery() {
        try {
            IOManager IOManager = new IOManager();

            if (!IOManager.databaseExists(this.databaseName)) {
                Utils.printMissingDatabaseError(databaseName);
                return false;
            }

            if (IOManager.checkTableExists(this.databaseName, tableName)) {
                Utils.printDuplicateTableError(this.databaseName, tableName);
                return false;
            }

            if (isduplicateColumnsPresent(columns)) {
                Utils.printMessage("ERROR(102C): Table cannot have duplicate columns.");
                return false;
            }


            List<InternalColumn> columnsList = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) {
                InternalColumn internalColumn = new InternalColumn();

                Column column = columns.get(i);
                internalColumn.setName(column.name);
                internalColumn.setDataType(column.type.toString());

                if (hasPrimaryKey && i == 0) {
                    internalColumn.setPrimary(true);
                } else {
                    internalColumn.setPrimary(false);
                }

                if (hasPrimaryKey && i == 0) {
                    internalColumn.setNullable(false);
                } else if (column.isNull) {
                    internalColumn.setNullable(true);
                } else {
                    internalColumn.setNullable(false);
                }

                columnsList.add(internalColumn);
            }

            boolean status = IOManager.createTable(this.databaseName, tableName + DatabaseConstants.DEFAULT_FILE_EXTENSION);
            if (status) {
                CatalogDatabaseHelper databaseHelper = new CatalogDatabaseHelper();
                int startingRowId = databaseHelper.updateSystemTablesTable(this.databaseName, tableName, columns.size());
                boolean systemTableUpdateStatus = databaseHelper.updateSystemColumnsTable(this.databaseName, tableName, startingRowId, columnsList);

                if (!systemTableUpdateStatus) {
                    Utils.printMessage("ERROR(102T): Failed to create table " + tableName);
                    return false;
                }
            }
        }
        catch (InternalException e) {
            Utils.printMessage(e.getMessage());
            return false;
        }

        return true;
    }

    private boolean isduplicateColumnsPresent(ArrayList<Column> columnArrayList) {
        HashMap<String, Integer> map = new HashMap<>();
        for (int i = 0; i < columnArrayList.size(); i++) {
            Column column = columnArrayList.get(i);
            if (map.containsKey(column.name)) {
                return true;
            }
            else {
                map.put(column.name, i);
            }
        }

        return false;
    }
}
