import {
  IColumn,
  ISchemaMetaData,
  ITableMetaData,
  KeyObject,
} from "~~/interfaces/types";

const FILE_FRAGMENT = "{ id, size, extension, url }";

export const buildRecordDetailsQueryFields = (
  schemas: Record<string, ISchemaMetaData>,
  schemaName: string,
  tableName: string
): string => {
  const schemaMetaData = schemas[schemaName];
  const tableMetaData = schemaMetaData.tables.find(
    (t: ITableMetaData) =>
      t.name.toLocaleLowerCase() === tableName.toLocaleLowerCase()
  );

  const allColumns = tableMetaData?.columns;
  const dataColumns = allColumns
    ?.filter((c) => !c.name.startsWith("mg_"))
    .filter((c) => c.columnType !== "HEADING");

  const refTableQueryFields = (refColumn: IColumn): string => {
    const refTableMetaData = schemas[
      refColumn.refSchema || schemaName
    ].tables.find((t: ITableMetaData) => t.name === refColumn.refTable);

    const allRefColumns = refTableMetaData?.columns;

    const refTableDataColumns = allRefColumns
      ?.filter((c) => !c.name.startsWith("mg_"))
      .filter((c) => c.columnType !== "HEADING");

    const refFields = refTableDataColumns?.map((column) => {
      switch (column.columnType) {
        case "STRING":
        case "TEXT":
          return column.name;
        case "FILE":
          return `${column.name} ${FILE_FRAGMENT}`;
        case "REF":
        case "ONTOLOGY":
        case "REF_ARRAY":
        case "REFBACK":
        case "ONTOLOGY_ARRAY":
          return ""; // stop recursion
        default:
          return column.name;
      }
    });

    const refQueryFields = refFields ? refFields.join(" ") : "";

    return refQueryFields;
  };

  const fields = dataColumns?.map((column) => {
    switch (column.columnType) {
      case "STRING":
      case "TEXT":
        return column.name;
      case "FILE":
        return `${column.name} ${FILE_FRAGMENT}`;
      case "REF":
      case "ONTOLOGY":
      case "REF_ARRAY":
      case "REFBACK":
      case "ONTOLOGY_ARRAY":
        return `${column.name} { ${refTableQueryFields(column)} }`;
      default:
        return column.name;
    }
  });

  const queryFields = fields ? fields.join(" ") : "";

  return queryFields;
};

export const buildRecordListQueryFields = (
  tableName: string,
  schemaName: string,
  schemas: Record<string, ISchemaMetaData>
) => {
  const keyFields = buildKeyFields(tableName, schemaName, schemas);
  const tableMetaData = getTableMetaData(schemas[schemaName], tableName);

  if (tableMetaData === undefined) {
    throw new Error(
      "buildRecordListQueryFields; tableMetaData is undefined for tableName " +
        tableName +
        " in schema " +
        schemaName
    );
  }

  const typeFields = tableMetaData.columns.map((c) => c.name);

  // suggested list fields that are part of this tableType
  const additionalFields: any = [
    "id",
    "name",
    "label",
    "description",
    "pid",
    "acronym",
    "logo",
  ].filter((value) => typeFields.includes(value));

  // Special case for logo, expand to include all fields
  if (additionalFields[additionalFields.length - 1] === "logo") {
    additionalFields.push(["id", "size", "extension", "url"]);
  }

  const queryFields = [...new Set([...keyFields, ...additionalFields])];

  const fieldsString = fieldsToQueryString(queryFields);

  return fieldsString;
};

const fieldsToQueryString = (fields: string[][]): string => {
  return fields.reduce((acc: string, field: any) => {
    if (Array.isArray(field)) {
      return (acc += " { " + fieldsToQueryString(field) + " } ");
    } else {
      return (acc += " " + field);
    }
  }, "");
};

const buildKeyFields = (
  tableName: string,
  schemaName: string,
  schemas: Record<string, ISchemaMetaData>
) => {
  const schemaMetaData = schemas[schemaName];
  const tableMetaData = getTableMetaData(schemaMetaData, tableName);

  const keyFields = tableMetaData.columns.reduce(
    (acc: any, column: IColumn) => {
      if (column.key === 1) {
        if (isValueType(column)) {
          acc.push(column.name);
        } else if (isRefType(column)) {
          if (!column.refTable) {
            throw new Error(
              "refTable is undefined for refColumn with name " +
                column.name +
                " in table " +
                tableName +
                ""
            );
          } else {
            acc.push(column.name);
            acc.push(
              buildKeyFields(
                column.refTable,
                column.refSchema || schemaName,
                schemas
              )
            );
          }
        } else if (isArrayType(column)) {
          console.log(
            "TODO: buildRecordListQueryFields, key column isArrayType, skip for now"
          );
        } else if (isFileType(column)) {
          console.log(
            "TODO: buildRecordListQueryFields, key column isFileType, skip for now"
          );
        } else {
          console.log(
            "TODO: buildRecordListQueryFields, key column is unknown type, skip for now"
          );
        }
      }
      return acc;
    },
    []
  );
  return keyFields || [];
};

export const extractExternalSchemas = (schemaMetaData: ISchemaMetaData) => {
  return [
    ...new Set(
      schemaMetaData.tables.reduce((acc: string[], table: ITableMetaData) => {
        table.columns.forEach((column: IColumn) => {
          if (column.refSchema) {
            acc.push(column.refSchema);
          }
        });
        return acc;
      }, [])
    ),
  ];
};

export const extractKeyFromRecord = (
  record: any,
  tableName: string,
  schemaName: string,
  schemas: Record<string, ISchemaMetaData>
) => {
  const schemaMetaData = schemas[schemaName];
  const tableMetaData = getTableMetaData(schemaMetaData, tableName);

  const key = tableMetaData.columns.reduce((acc: any, column: IColumn) => {
    if (column.key === 1 && record[column.name]) {
      if (isValueType(column)) {
        acc[column.name] = record[column.name];
      } else if (isRefType(column)) {
        if (!column.refTable) {
          throw new Error(
            "refTable is undefined for refColumn with name " +
              column.name +
              " in table " +
              tableName +
              ""
          );
        } else {
          acc[column.name] = extractKeyFromRecord(
            record[column.name],
            column.refTable,
            column.refSchema || schemaName,
            schemas
          );
        }
      } else if (isArrayType(column)) {
        console.log(
          "TODO: extractKeyFromRecord, key column isArrayType, skip for now"
        );
      } else if (isFileType(column)) {
        console.log(
          "TODO: extractKeyFromRecord, key column isFileType, skip for now"
        );
      } else {
        console.log(
          "TODO: extractKeyFromRecord, key column is unknown type, skip for now"
        );
      }
    }
    return acc;
  }, {});
  return key || {};
};

export const buildFilterFromKeysObject = (keys: KeyObject) => {
  return Object.entries(keys).reduce(
    (
      acc: Record<string, object>,
      [key, value]: [string, string | KeyObject]
    ) => {
      acc[key] = { equals: [value] };
      return acc;
    },
    {}
  );
};

export const getTableMetaData = (
  schemaMetaData: ISchemaMetaData,
  tableName: string
): ITableMetaData => {
  const tableMetaData = schemaMetaData.tables.find(
    (t: ITableMetaData) =>
      t.name.toLocaleLowerCase() === tableName.toLocaleLowerCase()
  );

  if (tableMetaData === undefined) {
    const msg = "ERROR: tableMetaData is undefined for tableName " + tableName;
    console.log(msg);
    throw new Error(msg);
  }
  return tableMetaData;
};
