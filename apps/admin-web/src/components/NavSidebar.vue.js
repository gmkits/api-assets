import { useRoute } from 'vue-router';
const route = useRoute();
const navItems = [
    { path: '/', label: '📊 Dashboard' },
    { path: '/calendar', label: '📅 Calendar' },
    { path: '/import', label: '📥 Import' },
    { path: '/validate', label: '✅ Validate' },
    { path: '/diff', label: '🔀 Diff' },
    { path: '/publish', label: '🚀 Publish' },
    { path: '/playground', label: '🧪 Playground' },
];
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['nav-brand']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-link']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-link']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.nav, __VLS_intrinsicElements.nav)({
    ...{ class: "nav-sidebar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "nav-brand" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h2, __VLS_intrinsicElements.h2)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
    ...{ class: "nav-list" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.navItems))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({
        key: (item.path),
    });
    const __VLS_0 = {}.RouterLink;
    /** @type {[typeof __VLS_components.RouterLink, typeof __VLS_components.RouterLink, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        to: (item.path),
        ...{ class: "nav-link" },
        ...{ class: ({ active: __VLS_ctx.route.path === item.path }) },
    }));
    const __VLS_2 = __VLS_1({
        to: (item.path),
        ...{ class: "nav-link" },
        ...{ class: ({ active: __VLS_ctx.route.path === item.path }) },
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    __VLS_3.slots.default;
    (item.label);
    var __VLS_3;
}
/** @type {__VLS_StyleScopedClasses['nav-sidebar']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-brand']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-list']} */ ;
/** @type {__VLS_StyleScopedClasses['nav-link']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            route: route,
            navItems: navItems,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
