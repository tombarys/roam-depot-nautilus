import { toggleRenderComponent } from "./entry-helpers";
import { updateTemplateString } from "./entry-helpers";

const componentName = 'Nautilus' 
const codeBlockUID = `roam-render-${componentName}-cljs`;
const cssBlockUID = `roam-render-${componentName}-css`;
const renderStringStart = `{{[[roam/render]]:((${codeBlockUID}))`;
const replacementString = `{{${componentName}`; 
const disabledStr = `-disabled`;

const version = 'v1';
const titleblockUID = `roam-render-${componentName}`;
const cssBlockParentUID = `${componentName}-css-parent`;

const defaults = {'prefix-str': '', 'desc-length': 22, 'todo-duration': 15};

async function newRenderString(renderStringStart, extensionAPI, replacementKey, newValue) {
  const keys = ['prefix-str', 'desc-length', 'todo-duration'];
  let values = [];

  for (let key of keys) {
      if (key === replacementKey) {
          values.push(newValue);
      } else {
          let value = await extensionAPI.settings.get(key) || defaults[key];
          values.push(value);
      }
  }
  console.log("values are ", values);
  return values[0] + ' ' + renderStringStart + ' ' + values.slice(1).join(' ') + '}}';
}


async function onload({extensionAPI}) {
  const panelConfig = {
      tabTitle: componentName,
      settings: 
        [{id:   "prefix-str",
        name:   "Nautilus prefix",
        description: "Your custom text preceding every newly created Nautilus spiral. E.g. #Agenda.",
        action: {type:  "input",
                 placeholder: extensionAPI.settings.get('prefix-str') || defaults['prefix-str'],
                 onChange: async (evt) => {
                   let newString = await newRenderString(renderStringStart, extensionAPI, 'prefix-str', evt.target.value);
                   updateTemplateString(renderStringStart, newString);
                 // console.log("Input Changed!", evt); 
            }
          }
        },
        {id: "desc-length",
          name: "Maximum legend title length",
          description: "Legend length in characters. Longer titles will be truncated. Applies to newly inserted spirals only. Factory setting: 22.",
          action: {
            type: "select",
            default: extensionAPI.settings.get('desc-length') || defaults['desc-length'],
            items: [14, 16, 18, 20, 22, 24, 26, 28], // specify your default values here
            onChange: async (evt) => {
              let newString = await newRenderString(renderStringStart, extensionAPI, 'desc-length', evt);
              updateTemplateString(renderStringStart, newString);
              // console.log("Desc-length changed to: ", evt, " and the new renderString is", newString);
            },
          }
        },
        {id: "todo-duration",
          name: "Default TODO duration",
          description: "Default TODO duration in minutes. Used whenever you create a new TODO without specifying a duration. Applies to newly inserted spirals only. Factory setting: 15.",
          action: {
            type: "select",
            items: [5, 10, 15, 20, 25, 30], // specify your default values here
            onChange: async (evt) => {
              let newString = await newRenderString(renderStringStart, extensionAPI, 'todo-duration', evt);
              updateTemplateString(renderStringStart, newString);
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
    toggleRenderComponent(true, titleblockUID, cssBlockParentUID, version, renderStringStart, replacementString, cssBlockUID, codeBlockUID, componentName, disabledStr)
  }

  console.log(`load ${componentName} plugin`)
}

function onunload() {
  console.log(`unload ${componentName} plugin`)
  toggleRenderComponent(false, titleblockUID, cssBlockParentUID, version, renderStringStart, replacementString, cssBlockUID, codeBlockUID, componentName, disabledStr)
}

export default {
onload,
onunload
};