export interface IResource {
  id: string;
  pid: string;
  acronym: string;
  name: string;
  website: string;
  description: string;
  contacts: IContributor[];
  logo?: IUrlObject;
}
export interface ICohort {
  id: string;
  name: string;
  acronym?: string;
  description?: string;
  website?: string;
  logo?: IUrlObject;
  contactEmail?: string;
  institution?: {
    acronym: string;
  };
  type: INameObject[];
  collectionType: INameObject[];
  populationAgeGroups?: IOntologyNode[];
  startYear: number;
  endYear: number;
  countries: {
    name: string;
    order: number;
  }[];
  regions: {
    name: string;
    order: number;
  }[];
  numberOfParticipants: number;
  numberOfParticipantsWithSamples?: number;
  designDescription: string;
  designSchematic: IFile;
  design: {
    definition: string;
    name: string;
  };
  designPaper?: {
    title: string;
    doi: string;
  }[];
  inclusionCriteria?: IOntologyNode[];
  otherInclusionCriteria?: string;
  collectionEvents: ICollectionEvent[];
  additionalOrganisations: IPartner[];
  contacts: IContributor[];
  networks: INetwork[];
  releaseDescription?: string;
  linkageOptions?: string;
  dataAccessConditionsDescription?: string;
  dataAccessConditions?: { name: string }[];
  fundingStatement?: string;
  acknowledgements?: string;
  documentation?: IDocumentation[];
}

export interface IVariableBase {
  name: string;
  resource: {
    id: string;
  };
  dataset: {
    name: string;
    resource: {
      id: string;
    };
  };
  label: string;
  description?: string;
}

export interface IVariableDetails {
  unit?: IOntologyNode;
  format?: IOntologyNode;
}

export interface IVariableMappings {
  mappings?: IMapping[];
  repeats?: {
    name: string;
    mappings: IMapping[];
  }[];
}

export type IVariable = IVariableBase & IVariableDetails;
export type IVariableWithMappings = IVariable & IVariableMappings;

interface IFile {
  id?: string;
  size?: number;
  extension?: string;
  url?: string;
}

interface IDocumentation {
  name: string;
  description: string;
  url: string;
  file: IFile;
}

interface IPartner {
  id: string;
  acronym: string;
  website: string;
  name: string;
  description: string;
  logo: IUrlObject;
}

interface IContributor {
  roleDescription: string;
  firstName: string;
  lastName: string;
  prefix?: string;
  initials: string;
  email: string;
  title: INameObject;
  organisation: INameObject;
}

interface INameObject {
  name: string;
}

interface IUrlObject {
  url: string;
}

interface ICollectionEvent {
  name: string;
  description: string;
  startYear: INameObject;
  endYear: number;
  numberOfParticipants: number;
  ageGroups: INameObject[];
  definition: string;
  dataCategories: ICollectionEventCategory[];
  sampleCategories: ICollectionEventCategory[];
  areasOfInformation: ICollectionEventCategory[];
  standardizedTools: ICollectionEventCategory[];
  standardizedToolsOther: string;
  subcohorts: INameObject[];
  coreVariables: string[];
}

interface ICollectionEventCategory {
  name: string;
  parent?: INameObject;
  definition?: string;
}

interface ICollectionEventCategorySet {
  name: string;
  children?: ICollectionEventCategorySet[];
  definition?: string;
}

export interface INetwork {
  id: string;
  name: string;
  acronym?: string;
  description?: string;
  logo?: IUrlObject;
  website?: string;
}

interface ITreeNode {
  name: string;
  children?: ITreeNode[];
  parent?: string;
}

interface IOntologyNode extends ITreeNode {
  code?: string;
  definition?: string;
  ontologyTermURI?: string;
}

interface ISetting {
  key: string;
  value: string;
}

interface IBaseFilter {
  title: string;
  initialCollapsed?: boolean;
}

interface ISearchFilter extends IBaseFilter {
  columnType: "_SEARCH";
  search?: string;
}

export interface IFilter extends IBaseFilter {
  columnType: "_SEARCH" | "ONTOLOGY" | "REF_ARRAY";
  refTable?: string;
  columnName?: string;
  filterTable?: string;
  conditions?: [] | { [key: string]: string }[];
  searchTables?: string[];
  search?: string;
}

export interface IFormField {
  name: string;
  label: string;
  fieldValue: string; // value is taken by vue reactivity
  inputType: "string" | "textarea";
  hasError?: boolean;
  message?: string;
}

export interface IContactFormData {
  recipientsFilter: string;
  subject: string;
  body: string;
}

export enum INotificationType {
  light,
  dark,
  success,
  error,
  warning,
  info,
}

export interface ILocale {
  locale: string;
  value: string;
}

export interface IColumn {
  columnType: string;
  id: string;
  name: string;
  computed?: string;
  conditions?: string[];
  descriptions?: ILocale[];
  key?: number;
  labels?: ILocale[];
  position?: number;
  readonly?: string;
  refBack?: string;
  refLabel?: string;
  refLabelDefault?: string;
  refLink?: string;
  refSchema?: string;
  refTable?: string;
  required?: boolean;
  semantics?: string[];
  validation?: string;
  visible?: string;
}

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

export interface ISchemaMetaData {
  name: string;
  tables: ITableMetaData[];
}

export interface ISectionField {
  meta: IColumn;
  value: any;
}

export interface ISection {
  meta: IColumn;
  fields: ISectionField[];
}

// workaround needed as circular references are not supported for records
export type KeyObject = {
  [key: string]: KeyObject | string;
};

export interface IMapping {
  syntax: string;
  description: string;
  sourceDataset: {
    resource: {
      id: string;
    };
    name: string;
  };
  targetVariable: {
    dataset: {
      resource: {
        id: string;
      };
      name: string;
    };
    name: string;
  };
  match: {
    name: string;
  };
}

export type HarmonizationStatus = "unmapped" | "partial" | "complete";
