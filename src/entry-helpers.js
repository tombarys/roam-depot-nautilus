import clsjFile from "./component.cljs";

function removeTheBlock(uid){
    roamAlphaAPI.deleteBlock({"block":{"uid": uid}})
}

function uidForToday() {
    let roamDate = new Date(Date.now());
    let today = window.roamAlphaAPI.util.dateToPageTitle(roamDate);
    return today
}

function createPage(title){
    // creates the page if it does not exist
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
            {"string": `${componentName}`, 
             // [[${uidForToday()}]]`,
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


export function updateTemplateString(renderString, renderStringWSettings){ 
    let query = `[:find
        (pull ?node [:block/string :block/uid])
      :where
        [?page :node/title "roam/render"]
        [?node :block/page ?page]
        [?node :block/string ?node-String]
        [(clojure.string/includes? ?node-String "${renderString}")]
      ]`;
    
    let result = window.roamAlphaAPI.q(query).flat();
    result.forEach(block => {
        const updatedString = renderStringWSettings 
        window.roamAlphaAPI.updateBlock({
          block: {
            uid: block.uid,
            string: updatedString
          }
        });
    });
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

function replaceRenderStringsOnUnload(renderString, replacementString){
// replaces all render strings excluding these on the roam/render page
    let query = `[:find    
        (pull ?node [:block/string :node/title :block/uid])   
        :where   
          [?node :block/string ?node-String]   
          [(clojure.string/includes? ?node-String "${renderString}")]   
        (not        
            [?node :block/parents ?parent]       
            [?parent :node/title "roam/render"])]`;
    
    let result = window.roamAlphaAPI.q(query).flat();
    result.forEach(block => {
        // if (block.node.title === 'roam/render') return;
        const updatedString = block.string.replace(renderString, replacementString);
        window.roamAlphaAPI.updateBlock({
          block: {
            uid: block.uid,
            string: updatedString
          }
        });
    });
}

export function toggleRenderComponent(state, titleblockUID, version, renderString, replacementString, codeBlockUID, componentName, disabledStr) {
    let renderPageName = 'roam/render'
    if (state==true) {
        replaceRenderString('{{' + componentName + disabledStr, renderString), // replaces all {{Nautilus-disabled}} with render component call string
        createRenderBlock(renderPageName, titleblockUID, version, codeBlockUID, componentName)

    } else if(state==false){
        replaceRenderStringsOnUnload(renderString, replacementString + disabledStr),
        removeTheBlock(titleblockUID)
    }
}
