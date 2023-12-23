import componentCSSFile from "./component.css";
import clsjFile from "./component.cljs";

function removeCodeBlock(uid){
    roamAlphaAPI.deleteBlock({"block":{"uid": uid}})
}

function uidForToday() {
    let roamDate = new Date(Date.now());
    let today = window.roamAlphaAPI.util.dateToPageTitle(roamDate);
    return today
}

function createPage(title){
    // creates the roam/css page if it does not exist
    let pageUID = roamAlphaAPI.util.generateUID()
    roamAlphaAPI.data
        .page.create(
            {"page": 
                {"title": title, 
                "uid": pageUID}})
    return pageUID;
}

function getPageUidByPageTitle(title){
    return roamAlphaAPI.q(
        `[:find (pull ?e [:block/uid]) :where [?e :node/title "${title}"]]`
        )?.[0]?.[0].uid || null
}

function createRenderBlock(renderPageName, titleblockUID, version, codeBlockUID, componentName){
    let renderPageUID = getPageUidByPageTitle(renderPageName)|| createPage(renderPageName);
    let templateBlockUID = roamAlphaAPI.util.generateUID()
    let codeBlockHeaderUID = roamAlphaAPI.util.generateUID()
    let renderBlockUID = roamAlphaAPI.util.generateUID()

    // create the titleblock
    //Component Name [[January 12th, 2023]]
    roamAlphaAPI
    .createBlock(
        {"location": 
            {"parent-uid": renderPageUID, 
            "order": 0}, 
        "block": 
            {"string": `${componentName} [[${uidForToday()}]]`,
            "uid":titleblockUID,
            "open":true,
            "heading":3}})
    // create the template name block
    // Component Name vXX [[roam/templates]]
    roamAlphaAPI
    .createBlock(
        {"location": 
            {"parent-uid": titleblockUID, 
            "order": 0}, 
        "block": 
            {"string": `${componentName} ${version} [[roam/templates]]`,
            "uid":templateBlockUID,
            "open":true}})
    // create the render component block
    // {{roam/render:((diA0Fyj5m))}}
    roamAlphaAPI
    .createBlock(
        {"location": 
            {"parent-uid": templateBlockUID, 
            "order": 0}, 
        "block": 
            {"string": `{{[[roam/render]]:((${codeBlockUID})) }}`,
            "uid":renderBlockUID}})

    // create code header block
    roamAlphaAPI
    .createBlock(
        {"location": 
            {"parent-uid": titleblockUID, 
            "order": 'last'}, 
        "block": 
            {"string": `code`,
            "uid":codeBlockHeaderUID,
            "open":false}})

            // create codeblock for the component

    let cljs = clsjFile
                
    let blockString = "```clojure\n " + cljs + " ```"
    roamAlphaAPI
    .createBlock(
        {"location": 
            {"parent-uid": codeBlockHeaderUID, 
            "order": 0}, 
        "block": 
            {"uid": codeBlockUID,
            "string": blockString}})
    
}


function createCSSBlock(parentUID, cssBlockUID, cssFile, parentString){
    // creates the initial code block and its parent
    // adding this to the roam/css page so users can use it as an example
    // if roam/css page doesn't exist then create it
    let pageUID = getPageUidByPageTitle('roam/css') || createPage('roam/css');
    // create closed parent block
    roamAlphaAPI.createBlock(
        {"location": 
            {"parent-uid": pageUID, 
            "order": "last"}, 
        "block": 
            {"string": `${parentString} [[${uidForToday()}]]`,
            "uid":parentUID,
            "open":false,
            "heading":3}})

    // create codeblock for the component
    // I do this so that a user can see what to customize
    let css = cssFile.toString();
    
    let blockString = "```css\n " + css + " ```"
    roamAlphaAPI
    .createBlock(
        {"location": 
            {"parent-uid": parentUID, 
            "order": 0}, 
        "block": 
            {"uid": cssBlockUID,
            "string": blockString}})

}

function replaceRenderString(renderString, replacementString){
    // replaces the {{[[roam/render]]:((5juEDRY_n))}} string across the entire graph
    // I do this because when the original block is deleted Roam leaves massive codeblocks wherever it was ref'd
    // also allows me to re-add back if a user uninstalls and then re-installs
    

    let query = `[:find
        (pull ?node [:block/string :node/title :block/uid])
      :where
        (or [?node :block/string ?node-String]
      [?node :node/title ?node-String])
        [(clojure.string/includes? ?node-String "${renderString}")]
      ]`;
    
    let result = window.roamAlphaAPI.q(query).flat();
    result.forEach(block => {
        const updatedString = block.string.replace(renderString, replacementString);
        window.roamAlphaAPI.updateBlock({
          block: {
            uid: block.uid,
            string: updatedString
          }
        });
    });
}


export function toggleRenderComponent(state, titleblockUID, cssBlockParentUID, version, renderString, replacementString, cssBlockUID, codeBlockUID, componentName) {
    let renderPageName = 'roam/render'
    if (state==true) {
        createRenderBlock(renderPageName, titleblockUID, version, codeBlockUID, componentName)
        createCSSBlock(cssBlockParentUID, cssBlockUID, componentCSSFile, `${componentName} STYLE`);

    } else if(state==false){
        replaceRenderString(renderString, replacementString)
        removeCodeBlock(titleblockUID)
        removeCodeBlock(cssBlockParentUID)
    }
}
