export const initialBiobankColumns = [
  { label: "Id:", column: "id", type: "string", showCopyIcon: true },
  {
    label: "PID:",
    column: "pid",
    type: "string",
    showCopyIcon: true,
    copyValuePrefix: "http://hdl.handle.net/",
  },
  { label: "Description:", column: "description", type: "longtext" },
  {
    label: "Quality labels:",
    column: {
      quality: [
        "label",
        "certification_report",
        "certification_image_link",
        "certification_number",
      ],
    },
    type: "quality",
    showOnBiobankCard: true,
  },
  {
    label: "Collection types:",
    column: { collections: [{ type: ["name", "label"] }] },
    type: "array",
    showOnBiobankCard: true,
  },
  {
    label: "Juridical person:",
    column: "juridical_person",
    type: "string",
    showOnBiobankCard: true,
  },
  {
    label: "Biobank capabilities:",
    column: { capabilities: ["name", "label"] },
    type: "array",
    showOnBiobankCard: true,
  },
];

export default initialBiobankColumns;
