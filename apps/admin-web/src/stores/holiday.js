import { defineStore } from 'pinia';
import { ref } from 'vue';
import { HolidayApiClient } from '@holiday/web-client';
const client = new HolidayApiClient({
    baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:3000',
    defaultRegion: 'CN',
});
export const useHolidayStore = defineStore('holiday', () => {
    const regions = ref([]);
    const manifest = ref(null);
    const loading = ref(false);
    const error = ref(null);
    async function fetchRegions() {
        loading.value = true;
        error.value = null;
        try {
            regions.value = await client.getRegions();
        }
        catch (e) {
            error.value = e instanceof Error ? e.message : String(e);
        }
        finally {
            loading.value = false;
        }
    }
    async function fetchManifest() {
        loading.value = true;
        error.value = null;
        try {
            manifest.value = await client.getManifest();
        }
        catch (e) {
            error.value = e instanceof Error ? e.message : String(e);
        }
        finally {
            loading.value = false;
        }
    }
    async function getDayInfo(date, region) {
        loading.value = true;
        error.value = null;
        try {
            return await client.getDayInfo(date, region);
        }
        catch (e) {
            error.value = e instanceof Error ? e.message : String(e);
            return null;
        }
        finally {
            loading.value = false;
        }
    }
    async function getYear(year, region) {
        loading.value = true;
        error.value = null;
        try {
            return await client.getYear(year, region);
        }
        catch (e) {
            error.value = e instanceof Error ? e.message : String(e);
            return [];
        }
        finally {
            loading.value = false;
        }
    }
    return {
        regions,
        manifest,
        loading,
        error,
        fetchRegions,
        fetchManifest,
        getDayInfo,
        getYear,
    };
});
