import { ref } from 'vue';
import { HolidayCalendar } from '@holiday/vue';
import DayDetail from '../components/DayDetail.vue';
const year = ref(new Date().getFullYear());
const month = ref(new Date().getMonth() + 1);
const region = ref('CN');
const selectedDay = ref(null);
function onDayClick(day) {
    selectedDay.value = day;
}
function prevMonth() {
    if (month.value === 1) {
        month.value = 12;
        year.value--;
    }
    else {
        month.value--;
    }
}
function nextMonth() {
    if (month.value === 12) {
        month.value = 1;
        year.value++;
    }
    else {
        month.value++;
    }
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "calendar-view" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "controls" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.region),
    type: "text",
    ...{ class: "input" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "month-nav" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.prevMonth) },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "month-label" },
});
(__VLS_ctx.year);
(String(__VLS_ctx.month).padStart(2, '0'));
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.nextMonth) },
    ...{ class: "btn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "calendar-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "calendar-panel" },
});
const __VLS_0 = {}.HolidayCalendar;
/** @type {[typeof __VLS_components.HolidayCalendar, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onDayClick': {} },
    year: (__VLS_ctx.year),
    month: (__VLS_ctx.month),
    regionCode: (__VLS_ctx.region),
}));
const __VLS_2 = __VLS_1({
    ...{ 'onDayClick': {} },
    year: (__VLS_ctx.year),
    month: (__VLS_ctx.month),
    regionCode: (__VLS_ctx.region),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onDayClick: (__VLS_ctx.onDayClick)
};
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "detail-panel" },
});
/** @type {[typeof DayDetail, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(DayDetail, new DayDetail({
    day: (__VLS_ctx.selectedDay),
}));
const __VLS_9 = __VLS_8({
    day: (__VLS_ctx.selectedDay),
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
/** @type {__VLS_StyleScopedClasses['calendar-view']} */ ;
/** @type {__VLS_StyleScopedClasses['controls']} */ ;
/** @type {__VLS_StyleScopedClasses['input']} */ ;
/** @type {__VLS_StyleScopedClasses['month-nav']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['month-label']} */ ;
/** @type {__VLS_StyleScopedClasses['btn']} */ ;
/** @type {__VLS_StyleScopedClasses['calendar-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['calendar-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['detail-panel']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            HolidayCalendar: HolidayCalendar,
            DayDetail: DayDetail,
            year: year,
            month: month,
            region: region,
            selectedDay: selectedDay,
            onDayClick: onDayClick,
            prevMonth: prevMonth,
            nextMonth: nextMonth,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
