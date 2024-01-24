import { toggleRenderComponent } from "./entry-helpers";
import { toggleAutorunComponent } from "./entry-helpers";
import { updateTemplateString } from "./entry-helpers";

const componentName = 'Nautilus'; // this is the name of the main Nautilus component that will be inserted into the graph
const codeBlockUID = `roam-render-${componentName}-cljs`; // this is the UID of the code block that contains the ClojureScript code for the component
const titleblockUID = `roam-render-${componentName}`; // this is the UID of the title block that contains the link to the roam/templates page
const renderStringStart = `{{[[roam/render]]:((${codeBlockUID}))`; // this is the start of the render string that will be inserted into the graph
const replacementStringStart = `{{${componentName}`; // this is the string that will replace the renderStringStart in the graph after uninstalling the plugin
const disabledStr = `-disabled`; // this is the string that will be appended to the componentName to disable the component

const componentPasteName = 'NautiPaste'; // secondary, optional helper component for pasting iCal events into the graph
const codeBlockPasteUID = `roam-render-${componentPasteName}-cljs`; // this is the UID of the code block that contains the ClojureScript code for the component
const titleblockPasteUID = `roam-render-${componentPasteName}`; // this is the UID of the title block that contains the link to the roam/templates page
const renderStringPasteStart = `{{[[roam/render]]:((${codeBlockPasteUID}))`; // this is the start of the render string that will be inserted into the graph
const replacementStringPasteStart = `{{${componentPasteName}`; // this is the string that will replace the renderStringStart in the graph after uninstalling the plugin


const version = 'v1';

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
  // console.log("values are ", values);
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
                 default: defaults['prefix-str'],
                 // placeholder: extensionAPI.settings.get('prefix-str') || defaults['prefix-str'],
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
            default: defaults['desc-length'],
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
            default: defaults['todo-duration'],
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
      if (!extensionAPI.settings.get(key)) {
          extensionAPI.settings.set(key, defaults[key])};
    }
  }

  setDefaultSettings(extensionAPI, defaults);
  extensionAPI.settings.panel.create(panelConfig);

  if (!roamAlphaAPI.data.pull("[*]", [":block/uid", titleblockUID])) {
    // component hasn't been loaded so we add it to the graph
    toggleRenderComponent(true, titleblockUID, version, renderStringStart, replacementStringStart, codeBlockUID, componentName, disabledStr, 'roam/render')
  }

  if (!roamAlphaAPI.data.pull("[*]", [":block/uid", titleblockPasteUID])) {
    // component hasn't been loaded so we add it to the graph
    toggleAutorunComponent(true, titleblockPasteUID, version, renderStringPasteStart, 
      replacementStringPasteStart, codeBlockPasteUID, componentPasteName, disabledStr, 'roam/cljs')
  }

  console.log(`load ${componentName} plugin`)
}

function onunload() {
  console.log(`unload ${componentName} plugin`)
  toggleRenderComponent(false, titleblockUID, version, renderStringStart, replacementStringStart, codeBlockUID, componentName, disabledStr, 'roam/render')
}

export default {
onload,
onunload
};