import cljsFile from "./component.cljs";
import { getTemplateString } from "./index.js";


async function removeTheBlock(uid){
    roamAlphaAPI.deleteBlock({"block":{"uid": uid}})
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

function getBlockContentStringByUID(uid){
    return roamAlphaAPI.q(
        `[:find (pull ?e [:block/string]) :where [?e :block/uid "${uid}"]]`
        )?.[0]?.[0].string || null
}


async function createRenderBlock(renderPageName, titleblockUID, version, codeBlockUID, componentName, templateString){
    let renderPageUID = getPageUidByPageTitle(renderPageName)|| createPage(renderPageName);
    let templateBlockUID = roamAlphaAPI.util.generateUID()
    let codeBlockHeaderUID = roamAlphaAPI.util.generateUID()
    let renderBlockUID = roamAlphaAPI.util.generateUID()

    // create the titleblock
    //Component Name
    roamAlphaAPI.createBlock(
        {"location": 
            {"parent-uid": renderPageUID, 
            "order": 0}, 
        "block": 
            {"string": `${componentName}`, // old: [[${uidForToday()}]]`,
            "uid":titleblockUID,
            "open":true,
            "heading":3}})
    // create the template name block
    // Component Name vXX [[roam/templates]]
    roamAlphaAPI.createBlock(
        {"location": 
            {"parent-uid": titleblockUID, 
            "order": 0}, 
        "block": 
            {"string": `${componentName} ${version} [[roam/templates]]`,
            "uid":templateBlockUID,
            "open":true}})
    // create the render component block
    // {{roam/render:((diA0Fyj5m))}}
    roamAlphaAPI.createBlock(
        {"location": 
            {"parent-uid": templateBlockUID, 
            "order": 0}, 
        "block": 
            {"string": templateString, 
            "uid":renderBlockUID}})

    // create code header block
    roamAlphaAPI.createBlock(
        {"location": 
            {"parent-uid": titleblockUID, 
            "order": 'last'}, 
        "block": 
            {"string": `code`,
            "uid":codeBlockHeaderUID,
            "open":false}})

    // create codeblock for the component
    let cljs = cljsFile
    let blockString = "```clojure\n " + cljs + " ```"
    await roamAlphaAPI.createBlock(
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


function replaceRenderString(searchString, replacementString){
    // replaces the {{[[roam/render]]:((5juEDRY_n))}} string across the entire graph
    // I do this because when the original block is deleted Roam leaves massive codeblocks wherever it was ref'd
    // also allows me to re-add back if a user uninstalls and then re-installs  

    let query = `[:find
        (pull ?node [:block/string :node/title :block/uid])
      :where
        (or [?node :block/string ?node-String]
      [?node :node/title ?node-String])
        [(clojure.string/includes? ?node-String "${searchString}")]
      ]`;
    
    let result = window.roamAlphaAPI.q(query).flat();
    result.forEach(block => {
        const updatedString = block.string.replace(searchString, replacementString);
        window.roamAlphaAPI.updateBlock({
          block: {
            uid: block.uid,
            string: updatedString
          }
        });
    });
}

async function updateBlockContentByUID(uid, content){
    roamAlphaAPI.updateBlock({"block": {"uid": uid, "string": content}});
}

export function toggleRenderComponent(state, titleblockUID, version, renderStringCore, disabledReplacementString, codeBlockUID, componentName, templateString) {
    let renderPageName = 'roam/render'
    if (state==true) {
        replaceRenderString(disabledReplacementString, renderStringCore); // replaces all {{Nautilus-disabled}} with render component call string – backward compatibility with older versions
        if (!roamAlphaAPI.data.pull("[*]", [":block/uid", codeBlockUID])) { // if the code block does not exist 
            removeTheBlock(titleblockUID); // remove the remains if these exist
            createRenderBlock(renderPageName, titleblockUID, version, codeBlockUID, componentName, templateString);
            console.log(`load ${componentName} plugin anew`);
        } else { // if the code block already exists
            // check if the codeBlockUIDs child content is the same as the current code block and update if not
            if ((getBlockContentStringByUID(codeBlockUID)) != "```clojure\n " + cljsFile + " ```") {
                updateBlockContentByUID(codeBlockUID, "```clojure\n " + cljsFile + " ```");
                console.log(`load ${componentName} plugin via update`);
            }
        }
    } else if (state = false) {
        // TODO: since we're not doing anything on state=false maybe call this fn onLoadHelper?
    }
}
