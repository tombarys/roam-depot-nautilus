import { toggleRenderComponent } from "./entry-helpers";
import { updateTemplateString as updateRenderStringEverywhere } from "./entry-helpers";

const componentName = 'Nautilus' 
const codeBlockUID = `roam-render-${componentName}-cljs`;
const cssBlockUID = `roam-render-${componentName}-css`;
const renderStringStart = `{{[[roam/render]]:((${codeBlockUID}))`;
const replacementString = `{{${componentName}`; 

const version = 'v1';
const titleblockUID = `roam-render-${componentName}`;
const cssBlockParentUID = `${componentName}-css-parent`;

const defaults = {'desc-length': 22, 'todo-duration': 15};

async function newRenderString(renderStringStart, extensionAPI, replacementKey, newValue) {
  const keys = ['desc-length', 'todo-duration']; // Add more keys as needed
  let values = [];

  for (let key of keys) {
      if (key === replacementKey) {
          values.push(newValue);
      } else {
          let value = await extensionAPI.settings.get(key) || defaults[key];
          values.push(value);
      }
  }
  return renderStringStart + ' '  + values.join(' ') + '}}';
}
async function onload({extensionAPI}) {
  const panelConfig = {
    tabTitle: componentName,
    settings: [{id: "desc-length",
                name: "Todo/event title length",
                description: "Title length (description) in characters. Longer titles will be truncated.",
                action: {
                  type: "select",
                  default: extensionAPI.settings.get('desc-length') || defaults['desc-length'],
                  items: [15, 18, 20, 22, 25], // specify your default values here
                  onChange: async (evt) => {
                    let newString = await newRenderString(renderStringStart, extensionAPI, 'desc-length', evt);
                    updateRenderStringEverywhere(renderStringStart, newString);
                    // console.log("Desc-length changed to: ", evt, " and the new renderString is", newString);
                  },
                }
              },
              {id: "todo-duration",
                name: "Default TODO duration",
                description: "Default TODO duration in minutes. This is used when you create a new TODO without specifying a duration.",
                action: {
                  type: "select",
                  items: [5, 10, 15, 20, 25, 30], // specify your default values here
                  onChange: async (evt) => {
                    let newString = await newRenderString(renderStringStart, extensionAPI, 'todo-duration', evt);
                    updateRenderStringEverywhere(renderStringStart, newString);
                    // console.log("Todo duration changed to: ", evt, " and the new renderString is", newString);
                  },
                }
              },
          ]
        };

  function setDefaultSettings(extensionAPI, defaults) {
    const keys = Object.keys(defaults);
    for (let key of keys) {
      extensionAPI.settings.set(key, extensionAPI.settings.get(key) || defaults[key]);
    }
  }

  setDefaultSettings(extensionAPI, defaults);
  extensionAPI.settings.panel.create(panelConfig);

  if (!roamAlphaAPI.data.pull("[*]", [":block/uid", titleblockUID])) {
    // component hasn't been loaded so we add it to the graph
    toggleRenderComponent(true, titleblockUID, cssBlockParentUID, version, renderStringStart, replacementString, cssBlockUID, codeBlockUID, componentName)
  }

  console.log(`load ${componentName} plugin`)
}

function onunload() {
  console.log(`unload ${componentName} plugin`)
  toggleRenderComponent(false, titleblockUID, cssBlockParentUID, version, renderStringStart, replacementString, cssBlockUID, codeBlockUID, componentName)
}

export default {
onload,
onunload
};