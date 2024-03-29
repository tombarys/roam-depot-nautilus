import { toggleRenderComponent } from "./entry-helpers";
import { updateTemplateString } from "./entry-helpers";

const componentName = 'Nautilus' 
const codeBlockUID = `roam-render-${componentName}-cljs`;
const renderStringCore = `{{[[roam/render]]:((${codeBlockUID}))`;
const disabledStr = `-disabled`;
const disabledReplacementString = `{{${componentName}${disabledStr}`;

const version = 'v1';
const titleblockUID = `roam-render-${componentName}`;

const defaults = {'prefix-str': '', 'desc-length': 22, 'todo-duration': 15};

async function newRenderString(renderStringCore, extensionAPI, replacementKey, newValue) {
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
  // console.log("values are ", values);
  return values[0] + ' ' + renderStringCore + ' ' + values.slice(1).join(' ') + '}}';
}

async function getTemplateString(extensionAPI) { // returns the whole template string for the render block (if all settings are not default else returns the default string)
  const keys = ['prefix-str', 'desc-length', 'todo-duration'];
  let values = [];
  let allAreDefault = true;
  for (let key of keys) {
          let value = await extensionAPI.settings.get(key);
          switch(value) {
            case defaults[key]: { 
              values.push(value);
              break; 
            }
            case undefined: {
              values.push(value); 
              break;
            }
            case null: {
              values.push(value); 
              break;
            }
            default: { 
              allAreDefault = false;
              values.push(value);
          }
        }
      }
  console.log("values are ", values, " and allAreDefault is ", allAreDefault);
  if (allAreDefault) { 
    return renderStringCore + '}}'; } 
  else {
    let trimmedValue = values[0].trim();
    let finalString = trimmedValue ? trimmedValue + ' ' : trimmedValue;
    return finalString + renderStringCore + ' ' + values.slice(1).join(' ') + '}}';
  }
}

async function onload({extensionAPI}) {
  const panelConfig = {
      tabTitle: componentName,
      settings: 
        [{id:   "prefix-str",
        name:   "Nautilus prefix",
        description: "Your custom text preceding every newly created Nautilus spiral. E.g. #Agenda.",
        action: {type:  "input",
                 default: defaults['prefix-str'],
                 // placeholder: extensionAPI.settings.get('prefix-str') || defaults['prefix-str'],
                 onChange: async (evt) => {
                   let newString = await newRenderString(renderStringCore, extensionAPI, 'prefix-str', evt.target.value);
                   updateTemplateString(renderStringCore, newString.trim());
                 // console.log("Input Changed!", evt); 
            }
          }
        },
        {id: "desc-length",
          name: "Maximum legend title length",
          description: "Legend length in characters. Longer titles will be truncated. Applies to newly inserted spirals only. Factory setting: 22.",
          action: {
            type: "select",
            default: defaults['desc-length'],
            items: [14, 16, 18, 20, 22, 24, 26, 28], // specify your default values here
            onChange: async (evt) => {
              let newString = await newRenderString(renderStringCore, extensionAPI, 'desc-length', evt);
              updateTemplateString(renderStringCore, newString);
              // console.log("Desc-length changed to: ", evt, " and the new renderString is", newString);
            },
          }
        },
        {id: "todo-duration",
          name: "Default TODO duration",
          description: "Default TODO duration in minutes. Used whenever you create a new TODO without specifying a duration. Applies to newly inserted spirals only. Factory setting: 15.",
          action: {
            type: "select",
            default: defaults['todo-duration'],
            items: [5, 10, 15, 20, 25, 30], // specify your default values here
            onChange: async (evt) => {
              let newString = await newRenderString(renderStringCore, extensionAPI, 'todo-duration', evt);
              updateTemplateString(renderStringCore, newString);
              // console.log("Todo duration changed to: ", evt, " and the new renderString is", newString);
            },
          }
        },
    ]
  };

  function setDefaultSettings(extensionAPI, defaults) {
    const keys = Object.keys(defaults);
    for (let key of keys) {
      if (!extensionAPI.settings.get(key)) {
          extensionAPI.settings.set(key, defaults[key])};
    }
  }

  setDefaultSettings(extensionAPI, defaults);
  extensionAPI.settings.panel.create(panelConfig);

  if (!roamAlphaAPI.data.pull("[*]", [":block/uid", titleblockUID])) {
    // component hasn't been loaded so we add it to the graph
    toggleRenderComponent(true, titleblockUID, version, renderStringCore, disabledReplacementString, codeBlockUID, componentName, await getTemplateString(extensionAPI));
    // console.log("getting template string:" + await getTemplateString(extensionAPI));
  }

  console.log(`load ${componentName} plugin`)
}



function onunload() {
  console.log(`unload ${componentName} plugin`)
  toggleRenderComponent(false, titleblockUID, version, renderStringCore, disabledReplacementString, codeBlockUID, componentName, '')
}

export default {
onload,
onunload
};