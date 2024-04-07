import { toggleRenderComponent } from "./entry-helpers";
import { updateTemplateString } from "./entry-helpers";

const componentName = 'Nautilus' 
const codeBlockUID = `roam-render-${componentName}-cljs`;
const renderStringCore = `{{[[roam/render]]:((${codeBlockUID}))`;
const disabledStr = `-disabled`;
const disabledReplacementString = `{{${componentName}${disabledStr}`;

const version = 'v5';
const titleblockUID = `roam-render-${componentName}`;

const defaults = {'prefix-str': '', 'desc-length': 22, 'todo-duration': 15, 'workday-start': 8, 'color-1-trigger': ''};

if (!window.nautilusExtensionData) {  
  window.nautilusExtensionData = {};
} 

window.nautilusExtensionData.running = true;

async function generateUpdatedRenderString(renderStringCore, extensionAPI, replacementKey, newValue) {
  const keys = Object.keys(defaults);
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

async function generateTemplateString(extensionAPI) { // returns the whole template string for the render block (if all settings are not default else returns the default string)
  const keys = Object.keys(defaults);
  let values = [];
  let allAreDefault = true;
  for (let key of keys) {
          let value = await extensionAPI.settings.get(key);
          switch(value) {
            case defaults[key]: {
              if (key === 'color-1-trigger') { value = '\"' + value.replace(/ /g, '') + '\"'; };
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
              if (key === 'color-1-trigger') { value = '\"' + value.replace(/ /g, '') + '\"'; };
              values.push(value);
          }
        }
      }
  // console.log("values are ", values, " and allAreDefault is ", allAreDefault);
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
        [{id: "workday-start",
          name: "Default workday start time",
          description: "Default workday start time. Options are 6(am), 7(am) or 8(am) which is default. Applies to a newly inserted Nautiluses only.",
          action: {
            type: "select",
            default: defaults['workday-start'],
            items: [6, 7, 8], // specify your default values here
            onChange: async (evt) => {
              let newString = await generateUpdatedRenderString(renderStringCore, extensionAPI, 'workday-start', evt);
              updateTemplateString(renderStringCore, newString);
            },
          }
        },
        {id:   "prefix-str",
        name:   "Nautilus prefix",
        description: "Your custom text preceding every newly created Nautilus spiral. E.g. #Agenda.",
        action: {type:  "input",
                 default: defaults['prefix-str'],
                 onChange: async (evt) => {
                   let newString = await generateUpdatedRenderString(renderStringCore, extensionAPI, 'prefix-str', evt.target.value);
                   updateTemplateString(renderStringCore, newString.trim());
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
              let newString = await generateUpdatedRenderString(renderStringCore, extensionAPI, 'desc-length', evt);
              updateTemplateString(renderStringCore, newString);
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
              let newString = await generateUpdatedRenderString(renderStringCore, extensionAPI, 'todo-duration', evt);
              updateTemplateString(renderStringCore, newString);
              // console.log("Todo duration changed to: ", evt, " and the new renderString is", newString);
            },
          }
        },
        {id:   "color-1-trigger",
        name:   "Custom word for red color",
        description: "Beta. Custom word that causes todo/event to show in red. E.g. important, #red etc. WARNING: cannot contain spaces. ",
        action: {type:  "input",
                 default: defaults['color-1-trigger'],
                 onChange: async (evt) => {
                   let cleanedValue = evt.target.value.replace(/ /g, ''); // Remove spaces from input
                   let newString = await generateUpdatedRenderString(renderStringCore, extensionAPI, 'color-1-trigger', '\"' + cleanedValue + '\"');
                   updateTemplateString(renderStringCore, newString);
            }
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

  toggleRenderComponent(true, titleblockUID, version, renderStringCore, disabledReplacementString, codeBlockUID, componentName, await generateTemplateString(extensionAPI));
  
}

function onunload() {
  console.log(`unload ${componentName} plugin`)
  toggleRenderComponent(false, titleblockUID, version, renderStringCore, disabledReplacementString, codeBlockUID, componentName, '')
  window.nautilusExtensionData.running = false;
}

export default {
onload,
onunload
};