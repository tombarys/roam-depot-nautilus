import { toggleRenderComponent } from "./entry-helpers";

const componentName = 'NAME' // keep this short
const codeBlockUID = `roam-render-${componentName}-cljs`;
const cssBlockUID = `roam-render-${componentName}-css`;
const renderString = `{{[[roam/render]]:((${codeBlockUID}))`;
const replacementString = `{{${componentName}}}`;
const version = 'v1';
const titleblockUID = `roam-render-${componentName}`;
const cssBlockParentUID = `${componentName}-css-parent`;

function onload({extensionAPI}) {
  const panelConfig = {
    tabTitle: componentName,
    settings: [
        // {id:		  "strikethrough",
        //   name:		"Strikethrough DONE tasks",
        //   description: "Adds CSS to strike through DONE tasks",
        //   action:	  {type:	 "switch",
        //                 onChange: (evt) => { 
        //                   // toggleStrikethroughCSS(evt.target.checked); 
        //                   console.log("toggle strikethrough CSS!", evt.target.checked); }}}
    ]
  };

  extensionAPI.settings.panel.create(panelConfig);

  if (!roamAlphaAPI.data.pull("[*]", [":block/uid", titleblockUID])) {
    // component hasn't been loaded so we add it to the graph
    toggleRenderComponent(true, titleblockUID, cssBlockParentUID, version, renderString, replacementString, cssBlockUID, codeBlockUID, componentName)
  }

  console.log(`load ${componentName} plugin`)
}

function onunload() {
  console.log(`unload ${componentName} plugin`)
  toggleRenderComponent(false, titleblockUID, cssBlockParentUID, version, renderString, replacementString, cssBlockUID, codeBlockUID, componentName)
}

export default {
onload,
onunload
};