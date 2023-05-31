import { IColumn } from "./IColumn";
import { ILocale } from "./ILocales";
import { ISetting } from "./ISetting";

export interface ITableMetaData {
  id: string;
  name: string;
  tableType: string;
  columns: IColumn[];
  descriptions?: ILocale[];
  externalSchema: string;
  labels?: ILocale[];
  semantics?: string[];
  settings?: ISetting[];
}
